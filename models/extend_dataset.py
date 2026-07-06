# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "imagehash>=4.3.2",
#     "numpy>=2.4.4",
#     "pillow>=12.2.0",
#     "playwright>=1.59.0",
#     "protobuf>=7.34.1",
#     "requests>=2.33.1",
#     "sentencepiece>=0.2.1",
#     "torch>=2.11.0",
#     "transformers>=5.8.0",
# ]
# ///
import argparse
import asyncio
import html
import io
import random
import re
import shutil
import urllib.parse
from collections import Counter
from pathlib import Path
from urllib.parse import parse_qs, urljoin, urlparse

try:
    import imagehash
except ImportError:
    imagehash = None

import numpy as np
import requests
import torch
from PIL import Image, ImageOps

try:
    from playwright.async_api import TimeoutError as PlaywrightTimeoutError
    from playwright.async_api import async_playwright
except ImportError:
    PlaywrightTimeoutError = Exception
    async_playwright = None

try:
    from transformers import AutoModel, AutoProcessor
except ImportError:
    AutoModel = None
    AutoProcessor = None

# Paths and defaults
BASE_DIR = Path(__file__).resolve().parent
DATA_ROOT = BASE_DIR / 'data' / 'images'
CLASSES_PATH = BASE_DIR / 'classes.txt'
LABELS_PATH = BASE_DIR / 'labels.txt'
TARGET_IMAGES_PER_CLASS = 100
CANDIDATE_BATCH_SIZE = 100
MAX_CANDIDATES_PER_CLASS = 1000
MAX_EDGE = 512
MIN_SOURCE_EDGE = 256
SCROLL_ROUNDS = 10
PHASH_THRESHOLD = 5
MIN_TARGET_SCORE = 0.60
MIN_SCORE_MARGIN = 0.05
NEGATIVE_LABEL_COUNT = 7
SIGLIP_MODEL_ID = 'google/siglip-base-patch16-224'
DRY_RUN_DELETE = False
VERBOSE_LOGGING = True
USER_AGENT = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
VALID_SUFFIXES = {'.jpg', '.jpeg', '.png', '.webp', '.bmp'}

# Runtime state
session = requests.Session()
session.headers.update(
    {
        'User-Agent': USER_AGENT,
        'Accept-Language': 'en-US,en;q=0.9',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
    }
)
processor = None
model = None
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'
DTYPE = torch.float16 if DEVICE == 'cuda' else torch.float32
allowed_slugs = []
labels = []
slug_to_label = {}
loaded_image_resources = []


def read_lines(path):
    return [line.strip() for line in path.read_text(encoding='utf-8').splitlines() if line.strip()]


def log_event(*parts):
    if VERBOSE_LOGGING:
        print(*parts)


def list_image_files(class_dir):
    return sorted([path for path in class_dir.iterdir() if path.is_file() and path.suffix.lower() in VALID_SUFFIXES])


def sync_dataset_folders(data_root, valid_slugs, dry_run=False):
    data_root.mkdir(parents=True, exist_ok=True)

    valid_slug_set = set(valid_slugs)
    existing_dirs = sorted([path for path in data_root.iterdir() if path.is_dir()])
    deleted = []

    for path in existing_dirs:
        if path.name not in valid_slug_set:
            deleted.append(path.name)
            if not dry_run:
                shutil.rmtree(path)

    created = []
    for slug in valid_slugs:
        class_dir = data_root / slug
        if not class_dir.exists():
            created.append(slug)
            if not dry_run:
                class_dir.mkdir(parents=True, exist_ok=True)

    print(f'Deleted {len(deleted)} folders')
    if deleted:
        print(deleted)
    print(f'Created {len(created)} missing folders')
    return deleted, created


def resize_to_max_edge(image, max_edge=512):
    image = ImageOps.exif_transpose(image).convert('RGB')
    width, height = image.size
    largest_edge = max(width, height)
    if largest_edge <= max_edge:
        return image
    scale = max_edge / float(largest_edge)
    new_size = (max(1, round(width * scale)), max(1, round(height * scale)))
    return image.resize(new_size, Image.Resampling.LANCZOS)


def load_existing_hashes(class_dir):
    hashes = []
    for path in list_image_files(class_dir):
        try:
            with Image.open(path) as image:
                hashes.append(imagehash.phash(resize_to_max_edge(image, MAX_EDGE)))
        except Exception:
            continue
    return hashes


def is_near_duplicate(image, existing_hashes, threshold=PHASH_THRESHOLD):
    candidate_hash = imagehash.phash(image)
    for existing_hash in existing_hashes:
        if candidate_hash - existing_hash <= threshold:
            return True, candidate_hash
    return False, candidate_hash


def save_image_if_new(image, class_dir, existing_hashes):
    prepared = resize_to_max_edge(image, MAX_EDGE)
    duplicate, candidate_hash = is_near_duplicate(prepared, existing_hashes)
    if duplicate:
        return None, 'duplicate_phash'

    buffer = io.BytesIO()
    prepared.save(buffer, format='JPEG', quality=95, optimize=True)
    image_bytes = buffer.getvalue()
    filename = f"{sum(image_bytes) % 10**12:012d}_{candidate_hash}.jpg"
    output_path = class_dir / filename
    while output_path.exists():
        filename = f"{random.randint(0, 10**12 - 1):012d}_{candidate_hash}.jpg"
        output_path = class_dir / filename

    output_path.write_bytes(image_bytes)
    existing_hashes.append(candidate_hash)
    return output_path, 'saved'


def ensure_dependencies():
    missing = []
    if imagehash is None:
        missing.append('imagehash')
    if async_playwright is None:
        missing.append('playwright')
    if AutoModel is None or AutoProcessor is None:
        missing.append('transformers')

    if missing:
        raise ImportError(
            'Missing Python packages: ' + ', '.join(missing) + '. Install script dependencies in this environment before running the script.'
        )


def load_siglip():
    global processor, model

    if processor is not None and model is not None:
        return

    try:
        import sentencepiece  # noqa: F401
    except ImportError as exc:
        raise ImportError(
            'SentencePiece missing. Install script dependencies in this environment before running the script.'
        ) from exc

    processor = AutoProcessor.from_pretrained(SIGLIP_MODEL_ID)
    model = AutoModel.from_pretrained(SIGLIP_MODEL_ID, dtype=DTYPE)
    model.to(DEVICE)
    model.eval()
    print(f'SigLIP loaded on {DEVICE}')


def build_candidate_labels(target_slug, target_label, all_slug_to_label, negative_count=NEGATIVE_LABEL_COUNT):
    other_slugs = [slug for slug in all_slug_to_label if slug != target_slug]
    rng = random.Random(target_slug)
    sampled_slugs = rng.sample(other_slugs, k=min(negative_count, len(other_slugs)))
    sampled_labels = [all_slug_to_label[slug] for slug in sampled_slugs]
    return [target_label] + sampled_labels


def score_image_against_labels(image, candidate_labels):
    texts = [f'This is a photo of {label}.' for label in candidate_labels]
    inputs = processor(text=texts, images=image, padding='max_length', return_tensors='pt')
    inputs = {key: value.to(DEVICE) for key, value in inputs.items()}

    with torch.no_grad():
        outputs = model(**inputs)

    return torch.sigmoid(outputs.logits_per_image)[0].detach().float().cpu().numpy()


def image_matches_label(image, target_slug, target_label, all_slug_to_label):
    candidate_labels = build_candidate_labels(target_slug, target_label, all_slug_to_label)
    probs = score_image_against_labels(image, candidate_labels)
    best_idx = int(np.argmax(probs))
    target_score = float(probs[0])
    strongest_other = float(max(probs[1:])) if len(probs) > 1 else 0.0
    accepted = best_idx == 0 and target_score >= MIN_TARGET_SCORE and (target_score - strongest_other) >= MIN_SCORE_MARGIN
    return accepted, {
        'target_score': round(target_score, 4),
        'best_label': candidate_labels[best_idx],
        'best_score': round(float(probs[best_idx]), 4),
        'strongest_other': round(strongest_other, 4),
        'candidate_labels': candidate_labels,
    }


def build_google_image_search_url(query):
    params = {
        'tbm': 'isch',
        'hl': 'en',
        'q': query,
    }
    return 'https://www.google.com/search?' + urllib.parse.urlencode(params)


def extract_html_title(html_text):
    match = re.search(r'<title>(.*?)</title>', html_text, flags=re.IGNORECASE | re.DOTALL)
    if not match:
        return None
    return html.unescape(match.group(1)).strip()


async def maybe_accept_google_consent(page):
    button_labels = ['Accept all', 'I agree', 'Accept', 'Alle akzeptieren', 'Tout accepter']
    for label in button_labels:
        try:
            button = page.get_by_role('button', name=label)
            await button.first.click(timeout=1500)
            await page.wait_for_timeout(1000)
            log_event('  consent accepted:', label)
            return True
        except Exception:
            continue
    return False


async def wait_for_google_results(page):
    selectors = [
        'img.YQ4gaf',
        'img.rg_i',
        'a[href*="/imgres"]',
        '[data-lpage]',
        '[jsname="dTDiAc"]',
        'img[alt]',
    ]

    for selector in selectors:
        try:
            await page.wait_for_selector(selector, state='attached', timeout=5000)
            return selector
        except PlaywrightTimeoutError:
            continue
    return None


async def count_result_locators(page):
    selectors = {
        'img.YQ4gaf': 'img.YQ4gaf',
        'img.rg_i': 'img.rg_i',
        'img[alt]': 'img[alt]',
        'a[/imgres]': 'a[href*="/imgres"]',
    }
    counts = {}
    for name, selector in selectors.items():
        try:
            counts[name] = await page.locator(selector).count()
        except Exception:
            counts[name] = 0
    return counts


def is_direct_image_url(url):
    lowered = url.lower()
    if not lowered.startswith(('http://', 'https://')):
        return False
    if lowered.startswith('data:'):
        return False
    blocked_markers = (
        'google.com/search',
        '/imgres',
        'support.google.com',
        'accounts.google.com',
        'consent.google',
    )
    if any(marker in lowered for marker in blocked_markers):
        return False
    blocked_extensions = ('.html', '.htm', '.php', '.asp', '.aspx')
    if any(lowered.endswith(ext) for ext in blocked_extensions):
        return False
    image_markers = ('.jpg', '.jpeg', '.png', '.webp', '.gif', '.bmp', '.avif')
    return any(marker in lowered for marker in image_markers)


async def collect_http_image_urls(page, preview_only=False):
    resource_urls = get_loaded_image_resources(preview_only=preview_only)
    if resource_urls:
        return resource_urls

    expression = """() => {
        const preview = [];
        const fallback = [];
        const seen = new Set();

        const addTo = (bucket, value) => {
            if (!value || typeof value !== 'string') return;
            const trimmed = value.trim();
            if (!trimmed || seen.has(trimmed)) return;
            if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) return;
            seen.add(trimmed);
            bucket.push(trimmed);
        };

        const addSrcSetTo = (bucket, value) => {
            if (!value || typeof value !== 'string') return;
            value.split(',').forEach((part) => {
                const url = part.trim().split(/\\s+/)[0];
                addTo(bucket, url);
            });
        };

        const previewRoots = [
            '[role="dialog"]',
            '[data-lpage]',
            '[jsname="figiqf"]',
            '[jsname="dTDiAc"]',
        ];

        previewRoots.forEach((selector) => {
            document.querySelectorAll(selector).forEach((root) => {
                root.querySelectorAll('img').forEach((node) => {
                    addTo(preview, node.currentSrc);
                    addTo(preview, node.src);
                    addTo(preview, node.getAttribute('data-src'));
                    addTo(preview, node.getAttribute('data-iurl'));
                    addSrcSetTo(preview, node.srcset);
                    addSrcSetTo(preview, node.getAttribute('data-srcset'));
                });
                root.querySelectorAll('source').forEach((node) => {
                    addTo(preview, node.src);
                    addSrcSetTo(preview, node.srcset);
                });
            });
        });

        document.querySelectorAll('img').forEach((node) => {
            addTo(fallback, node.currentSrc);
            addTo(fallback, node.src);
            addTo(fallback, node.getAttribute('data-src'));
            addTo(fallback, node.getAttribute('data-iurl'));
            addSrcSetTo(fallback, node.srcset);
            addSrcSetTo(fallback, node.getAttribute('data-srcset'));
        });

        document.querySelectorAll('source').forEach((node) => {
            addTo(fallback, node.src);
            addSrcSetTo(fallback, node.srcset);
        });

        return { preview, fallback };
    }"""

    try:
        payload = await page.evaluate(expression)
    except Exception as exc:
        if 'Execution context was destroyed' in str(exc):
            await page.wait_for_load_state('domcontentloaded')
            await page.wait_for_timeout(500)
            payload = await page.evaluate(expression)
        else:
            raise

    ordered_urls = payload['preview'] if preview_only else [*payload['preview'], *payload['fallback']]
    cleaned = []
    seen = set()
    for url in ordered_urls:
        if not is_direct_image_url(url):
            continue
        if url in seen:
            continue
        seen.add(url)
        cleaned.append(url)
    return cleaned


def is_google_allowed_url(url):
    parsed = urlparse(url)
    host = (parsed.hostname or '').lower()
    if not host:
        return True
    allowed_suffixes = (
        'google.com',
        'google.ie',
        'gstatic.com',
        'googleusercontent.com',
    )
    return any(host == suffix or host.endswith('.' + suffix) for suffix in allowed_suffixes)


def normalize_resource_url(url):
    parsed = urlparse(url)
    return parsed._replace(fragment='').geturl()


def record_loaded_image_resource(url):
    normalized = normalize_resource_url(url)
    if is_direct_image_url(normalized):
        loaded_image_resources.append(normalized)


def clear_loaded_image_resources():
    loaded_image_resources.clear()


def get_loaded_image_resources(preview_only=False):
    ordered = []
    seen = set()
    candidates = reversed(loaded_image_resources) if preview_only else loaded_image_resources
    for url in candidates:
        if url in seen:
            continue
        seen.add(url)
        ordered.append(url)
    return ordered


async def install_google_request_block(page):
    async def handler(route):
        request = route.request
        if request.resource_type in {'image', 'media', 'font', 'stylesheet', 'script', 'xhr', 'fetch'}:
            await route.continue_()
            return
        if is_google_allowed_url(request.url):
            await route.continue_()
            return
        await route.abort()

    await page.route('**/*', handler)


async def wait_for_google_preview(page):
    selectors = [
        '[role="dialog"] img',
        '[data-lpage] img',
        '[jsname="figiqf"] img',
        '[jsname="dTDiAc"] img',
    ]
    for selector in selectors:
        try:
            await page.wait_for_selector(selector, state='attached', timeout=1500)
            return True
        except PlaywrightTimeoutError:
            continue
    return False


async def install_google_result_navigation_block(page):
    await page.evaluate(
        """() => {
            if (window.__google_result_nav_block_installed) return;
            window.__google_result_nav_block_installed = true;
            const blocker = (event) => {
                const anchor = event.target && event.target.closest ? event.target.closest('a') : null;
                if (!anchor) return;
                const href = anchor.getAttribute('href') || anchor.href || '';
                if (!href) return;
                if (href.startsWith('/imgres') || href.startsWith('http')) {
                    event.preventDefault();
                    event.stopPropagation();
                    event.stopImmediatePropagation();
                }
            };
            document.addEventListener('click', blocker, true);
            document.addEventListener('auxclick', blocker, true);
            document.addEventListener('mousedown', blocker, true);
        }"""
    )


async def click_result_thumbnail(page, index, click_limit=12):
    selectors = [
        'img.YQ4gaf',
        'img.rg_i',
        '[jsname="Q4LuWd"]',
    ]

    for selector in selectors:
        locator = page.locator(selector)
        count = await locator.count()
        if count == 0 or index >= min(count, click_limit):
            continue

        try:
            item = locator.nth(index)
            await item.scroll_into_view_if_needed(timeout=1000)
            await item.evaluate(
                """node => {
                    const events = ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'];
                    for (const type of events) {
                        node.dispatchEvent(new MouseEvent(type, {
                            bubbles: true,
                            cancelable: true,
                            view: window,
                            button: 0,
                            buttons: 1,
                        }));
                    }
                }"""
            )
            await page.wait_for_timeout(250)
            await wait_for_google_preview(page)
            return True
        except Exception:
            continue

    return False


async def maybe_click_more_results(page):
    labels = ['Show more results', 'More results', 'See more']
    for label in labels:
        try:
            button = page.get_by_role('button', name=label)
            await button.first.click(timeout=1500)
            await page.wait_for_timeout(1000)
            log_event('  clicked more results:', label)
            return True
        except Exception:
            continue
    return False


async def open_google_image_search(page, query):
    search_url = build_google_image_search_url(query)
    clear_loaded_image_resources()
    await page.goto(search_url, wait_until='domcontentloaded', timeout=60000)
    await page.wait_for_load_state('domcontentloaded')
    await page.wait_for_timeout(1500)
    log_event('  search url:', search_url)
    log_event('  page title:', await page.title())
    await maybe_accept_google_consent(page)
    await page.wait_for_timeout(1500)
    await install_google_request_block(page)
    await install_google_result_navigation_block(page)
    result_selector = await wait_for_google_results(page)
    counts = await count_result_locators(page)
    log_event('  result selector:', result_selector)
    log_event('  result counts:', counts)
    return result_selector


async def harvest_candidate_urls_for_query(page, query, seen_urls, batch_target, scroll_rounds):
    await open_google_image_search(page, query)

    collected = []
    idle_rounds = 0

    for round_idx in range(scroll_rounds):
        counts = await count_result_locators(page)
        fresh_urls = []
        clicked = 0

        for idx in range(12):
            opened = await click_result_thumbnail(page, idx)
            if not opened:
                continue
            clicked += 1
            await page.wait_for_timeout(400)

            for url in await collect_http_image_urls(page, preview_only=True):
                if url in seen_urls:
                    continue
                seen_urls.add(url)
                collected.append(url)
                fresh_urls.append(url)
                if len(collected) >= batch_target:
                    log_event(
                        f'  round {round_idx + 1}/{scroll_rounds}:',
                        f'clicked={clicked}',
                        f'fresh={len(fresh_urls)}',
                        f'total_new={len(collected)}',
                        f'counts={counts}'
                    )
                    return collected, False

        log_event(
            f'  round {round_idx + 1}/{scroll_rounds}:',
            f'clicked={clicked}',
            f'fresh={len(fresh_urls)}',
            f'total_new={len(collected)}',
            f'counts={counts}'
        )

        if fresh_urls:
            idle_rounds = 0
        else:
            idle_rounds += 1

        if idle_rounds >= 2:
            expanded = await maybe_click_more_results(page)
            if expanded:
                idle_rounds = 0
            elif idle_rounds >= 3:
                break

        await page.mouse.wheel(0, 5000)
        await page.wait_for_timeout(1200)

    exhausted = idle_rounds >= 3 or len(collected) < batch_target
    log_event('  collected batch urls:', len(collected))
    return collected, exhausted


def download_image(url):
    response = session.get(url, timeout=20)
    response.raise_for_status()
    content_type = response.headers.get('content-type', '').lower()
    if 'image' not in content_type:
        raise ValueError(f'URL did not return image content: {content_type}')
    image = Image.open(io.BytesIO(response.content))
    image.load()
    return image


def process_candidate_url(url, class_dir, target_slug, target_label, existing_hashes):
    try:
        image = download_image(url)
    except Exception as exc:
        return None, 'download_error', {'url': url, 'error': repr(exc)}

    if max(image.size) < MIN_SOURCE_EDGE:
        return None, 'too_small', {'url': url, 'size': image.size}

    resized = resize_to_max_edge(image, MAX_EDGE)
    accepted, score_info = image_matches_label(resized, target_slug, target_label, slug_to_label)
    score_info['url'] = url
    score_info['size'] = resized.size
    if not accepted:
        return None, 'siglip_reject', score_info

    output_path, save_reason = save_image_if_new(resized, class_dir, existing_hashes)
    score_info['saved_to'] = str(output_path) if output_path else None
    return output_path, save_reason, score_info


async def extend_one_class(page, slug, label, target_count=TARGET_IMAGES_PER_CLASS, candidate_batch_size=CANDIDATE_BATCH_SIZE, max_candidates_per_class=MAX_CANDIDATES_PER_CLASS, scroll_rounds=SCROLL_ROUNDS):
    class_dir = DATA_ROOT / slug
    class_dir.mkdir(parents=True, exist_ok=True)
    existing_files = list_image_files(class_dir)
    existing_hashes = load_existing_hashes(class_dir)
    final_count = len(existing_files)
    stats = Counter()
    stats['existing'] = final_count

    if final_count >= target_count:
        log_event(f'  skip: already at target with {final_count} images')
        return {
            'slug': slug,
            'label': label,
            'existing': final_count,
            'added': 0,
            'final': final_count,
            'rejections': {},
            'candidate_urls_seen': 0,
            'search_exhausted': False,
        }

    query = f'{label} food'
    seen_urls = set()
    exhausted = False
    processed_candidates = 0
    batch_number = 0

    while final_count < target_count and not exhausted:
        remaining_budget = max_candidates_per_class - len(seen_urls)
        if remaining_budget <= 0:
            log_event(f'  candidate budget exhausted at {max_candidates_per_class} URLs')
            break

        batch_number += 1
        batch_target = min(candidate_batch_size, remaining_budget)
        candidate_urls, exhausted = await harvest_candidate_urls_for_query(
            page,
            query,
            seen_urls=seen_urls,
            batch_target=batch_target,
            scroll_rounds=scroll_rounds,
        )

        log_event(
            f'  batch {batch_number}:',
            f'new_candidates={len(candidate_urls)}',
            f'total_candidates={len(seen_urls)}',
            f'current_saved={final_count}/{target_count}',
        )

        if not candidate_urls:
            log_event('  no candidate URLs found. Google returned no direct HTTP image URLs in the visible DOM.')
            break

        for url in candidate_urls:
            if final_count >= target_count:
                break

            processed_candidates += 1
            parsed = urlparse(url)
            short_url = f'{parsed.scheme}://{parsed.netloc}{parsed.path}' if parsed.scheme and parsed.netloc else url
            log_event(f'  candidate {processed_candidates}: {short_url}')
            output_path, result, details = process_candidate_url(url, class_dir, slug, label, existing_hashes)
            stats[result] += 1

            if result == 'saved':
                final_count += 1
                log_event(
                    f"    saved -> {output_path.name}",
                    f"score={details['target_score']}",
                    f"best={details['best_label']}",
                    f"other={details['strongest_other']}"
                )
            elif result == 'siglip_reject':
                log_event(
                    '    rejected by siglip',
                    f"score={details['target_score']}",
                    f"best={details['best_label']}",
                    f"best_score={details['best_score']}",
                    f"other={details['strongest_other']}"
                )
            elif result == 'too_small':
                log_event('    rejected too_small', details['size'])
            elif result == 'download_error':
                log_event('    download_error', details['error'])
            elif result == 'duplicate_phash':
                log_event('    rejected duplicate_phash')
            else:
                log_event('    result', result, details)

    log_event('  class summary:', dict(stats))
    return {
        'slug': slug,
        'label': label,
        'existing': stats['existing'],
        'added': stats['saved'],
        'final': final_count,
        'rejections': dict(stats),
        'candidate_urls_seen': len(seen_urls),
        'search_exhausted': exhausted,
    }


async def extend_dataset(selected_slugs=None, target_count=TARGET_IMAGES_PER_CLASS, candidate_batch_size=CANDIDATE_BATCH_SIZE, max_candidates_per_class=MAX_CANDIDATES_PER_CLASS, scroll_rounds=SCROLL_ROUNDS):
    run_slugs = selected_slugs or allowed_slugs
    results = []

    async with async_playwright() as playwright:
        try:
            browser = await playwright.firefox.launch(headless=False)
        except Exception as exc:
            raise RuntimeError(
                'Firefox launch failed. Install Playwright Firefox deps in this environment, then retry.'
            ) from exc

        context = await browser.new_context(user_agent=USER_AGENT, locale='en-US', viewport={'width': 1600, 'height': 1200})
        page = await context.new_page()

        page.on(
            'response',
            lambda response: record_loaded_image_resource(response.url)
            if response.request.resource_type == 'image'
            else None,
        )

        for idx, slug in enumerate(run_slugs, start=1):
            label = slug_to_label[slug]
            print(f'[{idx}/{len(run_slugs)}] {slug} -> {label}')
            result = await extend_one_class(
                page,
                slug,
                label,
                target_count=target_count,
                candidate_batch_size=candidate_batch_size,
                max_candidates_per_class=max_candidates_per_class,
                scroll_rounds=scroll_rounds,
            )
            results.append(result)
            print(
                f"  existing={result['existing']} added={result['added']} final={result['final']} "
                f"candidates={result['candidate_urls_seen']} exhausted={result['search_exhausted']}"
            )
            print(f"  rejection summary={result['rejections']}")

        await context.close()
        await browser.close()

    return results


def summarize_counts(selected_slugs=None):
    run_slugs = selected_slugs or allowed_slugs
    summary = []
    for slug in run_slugs:
        class_dir = DATA_ROOT / slug
        count = len(list_image_files(class_dir)) if class_dir.exists() else 0
        summary.append({'slug': slug, 'label': slug_to_label[slug], 'count': count})
    return summary


def parse_args():
    parser = argparse.ArgumentParser(description='Extend food dataset with Google Images + SigLIP filtering.')
    parser.add_argument(
        '--data-root',
        '--dataset-images-folder',
        dest='data_root',
        type=Path,
        default=DATA_ROOT,
        help='Path to dataset images root folder.',
    )
    parser.add_argument('--classes-path', type=Path, default=CLASSES_PATH)
    parser.add_argument('--labels-path', type=Path, default=LABELS_PATH)
    parser.add_argument('--target-images-per-class', type=int, default=TARGET_IMAGES_PER_CLASS)
    parser.add_argument('--candidate-batch-size', type=int, default=CANDIDATE_BATCH_SIZE)
    parser.add_argument('--max-candidates-per-class', type=int, default=MAX_CANDIDATES_PER_CLASS)
    parser.add_argument('--scroll-rounds', type=int, default=SCROLL_ROUNDS)
    parser.add_argument('--dry-run-delete', action='store_true')
    parser.add_argument('--slugs', nargs='*', help='Specific class slugs to process.')
    parser.add_argument('--limit', type=int, help='Only process the first N selected slugs.')
    parser.add_argument('--quiet', action='store_true')
    return parser.parse_args()


def configure_runtime(args):
    global DATA_ROOT, CLASSES_PATH, LABELS_PATH, TARGET_IMAGES_PER_CLASS
    global CANDIDATE_BATCH_SIZE, MAX_CANDIDATES_PER_CLASS, SCROLL_ROUNDS
    global DRY_RUN_DELETE, VERBOSE_LOGGING, allowed_slugs, labels, slug_to_label

    DATA_ROOT = args.data_root.resolve()
    CLASSES_PATH = args.classes_path.resolve()
    LABELS_PATH = args.labels_path.resolve()
    TARGET_IMAGES_PER_CLASS = args.target_images_per_class
    CANDIDATE_BATCH_SIZE = args.candidate_batch_size
    MAX_CANDIDATES_PER_CLASS = args.max_candidates_per_class
    SCROLL_ROUNDS = args.scroll_rounds
    DRY_RUN_DELETE = args.dry_run_delete
    VERBOSE_LOGGING = not args.quiet

    if not CLASSES_PATH.exists():
        raise FileNotFoundError(f'classes file not found: {CLASSES_PATH}')
    if not LABELS_PATH.exists():
        raise FileNotFoundError(f'labels file not found: {LABELS_PATH}')

    allowed_slugs = read_lines(CLASSES_PATH)
    labels = read_lines(LABELS_PATH)
    if len(allowed_slugs) != len(labels):
        raise ValueError(f'classes.txt has {len(allowed_slugs)} rows but labels.txt has {len(labels)} rows')

    slug_to_label = dict(zip(allowed_slugs, labels))
    print(f'Loaded {len(allowed_slugs)} classes and {len(labels)} labels')
    print('Sample:', allowed_slugs[:5], '->', labels[:5])


async def main_async(args):
    configure_runtime(args)
    ensure_dependencies()
    load_siglip()

    deleted_folders, created_folders = sync_dataset_folders(DATA_ROOT, allowed_slugs, dry_run=DRY_RUN_DELETE)

    selected_slugs = args.slugs or allowed_slugs
    for slug in selected_slugs:
        if slug not in slug_to_label:
            raise ValueError(f'Unknown slug: {slug}')

    if args.limit is not None:
        selected_slugs = selected_slugs[:args.limit]

    await extend_dataset(
        selected_slugs=selected_slugs,
        target_count=TARGET_IMAGES_PER_CLASS,
        candidate_batch_size=CANDIDATE_BATCH_SIZE,
        max_candidates_per_class=MAX_CANDIDATES_PER_CLASS,
        scroll_rounds=SCROLL_ROUNDS,
    )

    summary = summarize_counts(selected_slugs=selected_slugs)
    below_target = [row for row in summary if row['count'] < TARGET_IMAGES_PER_CLASS]
    print(f'Classes below target: {len(below_target)}')
    print('First 10 below target:', below_target[:10])
    print('Deleted folders:', deleted_folders)
    print('Created folders:', len(created_folders))


def main():
    args = parse_args()
    asyncio.run(main_async(args))


if __name__ == '__main__':
    main()
