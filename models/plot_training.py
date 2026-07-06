#!/usr/bin/env python3
"""
plot_training.py — Standalone training dashboard for InceptionV3 (Food-101)
Reads CSV logs + model_info.txt from  data/models/{MODEL_NAME}/
and renders a multi-panel dashboard PNG.

Usage:
    python plot_training.py                          # auto-detect first model
    python plot_training.py InceptionV3              # explicit model name
    python plot_training.py InceptionV3 --show       # also open the plot window
    python plot_training.py --base-dir /tf/data/models InceptionV3
"""

import argparse
import os
import sys
import re
from pathlib import Path

import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")           # headless by default; --show overrides below
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.gridspec import GridSpec

# ── CLI ───────────────────────────────────────────────────────────────────────
def parse_args():
    p = argparse.ArgumentParser(description="Plot InceptionV3 training dashboard from CSV logs")
    p.add_argument("model_name", nargs="?", default=None,
                   help="Model subdirectory name (e.g. InceptionV3). Auto-detected if omitted.")
    p.add_argument("--base-dir", default=os.path.join("data", "models"),
                   help="Root directory that contains model subdirs. Default: data/models")
    p.add_argument("--show", action="store_true",
                   help="Open interactive plot window after saving")
    p.add_argument("--dpi", type=int, default=150,
                   help="Output DPI (default 150)")
    p.add_argument("--out", default=None,
                   help="Output PNG path. Default: <model_dir>/training_dashboard.png")
    return p.parse_args()

# ── Discover model dir ────────────────────────────────────────────────────────
def resolve_model_dir(base_dir: str, model_name: str | None) -> tuple[Path, str]:
    base = Path(base_dir)
    if not base.exists():
        sys.exit(f"[ERROR] Base directory not found: {base.resolve()}")

    if model_name:
        d = base / model_name
        if not d.is_dir():
            sys.exit(f"[ERROR] Model directory not found: {d.resolve()}")
        return d, model_name

    # auto-detect: pick first subdir that contains a base log
    candidates = [
        d for d in sorted(base.iterdir())
        if d.is_dir() and any(d.glob("*history_base.log"))
    ]
    if not candidates:
        sys.exit(f"[ERROR] No model directories with history_base.log found under {base.resolve()}")
    chosen = candidates[0]
    print(f"[INFO] Auto-detected model: {chosen.name}")
    return chosen, chosen.name

# ── Load logs ─────────────────────────────────────────────────────────────────
def load_logs(model_dir: Path, model_name: str) -> tuple[pd.DataFrame, pd.DataFrame]:
    base_log = model_dir / f"{model_name}_history_base.log"
    ft_log   = model_dir / f"{model_name}_history_finetuned.log"

    if not base_log.exists():
        sys.exit(f"[ERROR] Base log not found: {base_log}")

    ht = pd.read_csv(base_log)
    ht["phase"] = "head"
    ht["epoch_global"] = ht["epoch"] + 1          # 1-indexed

    if ft_log.exists():
        ft = pd.read_csv(ft_log)
        ft["phase"] = "finetune"
        ft["epoch_global"] = ft["epoch"] + len(ht) + 1
    else:
        print("[WARN] Fine-tune log not found — plotting head-training only.")
        ft = pd.DataFrame()

    return ht, ft

# ── Parse model_info.txt ──────────────────────────────────────────────────────
def load_model_info(model_dir: Path, model_name: str) -> dict:
    info_path = model_dir / f"{model_name}_model_info.txt"
    info = {
        "model_name": model_name,
        "epochs": "?",
        "fine_tune_epochs": "?",
        "batch_size": "?",
        "img_size": "?",
        "n_classes": "?",
        "total_params": "?",
        "trainable_params": "?",
        "optimizer": "Adam",
        "lr": "?",
        "label_smoothing": "?",
        "head": "?",
        "augmentations": [],
    }
    if not info_path.exists():
        return info

    text = info_path.read_text()
    def _get(pattern, default="?"):
        m = re.search(pattern, text)
        return m.group(1).strip() if m else default

    info["model_name"]       = _get(r"MODEL_NAME\s*:\s*(.+)")
    info["epochs"]           = _get(r"EPOCHS\s*:\s*(\d+)")
    info["fine_tune_epochs"] = _get(r"FINE_TUNE_EPOCHS\s*:\s*(\d+)")
    info["batch_size"]       = _get(r"BATCH_SIZE\s*:\s*(\d+)")
    info["img_size"]         = _get(r"IMG_SIZE\s*:\s*(.+)")
    info["n_classes"]        = _get(r"N_CLASSES\s*:\s*(\d+)")
    info["lr"]               = _get(r"learning_rate\s*:\s*([\d.e+-]+)")
    info["label_smoothing"]  = _get(r"label_smoothing\s*:\s*([\d.]+)")
    info["head"]             = _get(r"-- Head -{10,}\s*\n(.+?)(?=\n--|$)", "?")

    # Total / trainable params
    m_tot  = re.search(r"Total params.*?([\d,]+)\s*\(", text)
    m_trn  = re.search(r"Trainable params.*?([\d,]+)\s*\(", text)
    if m_tot: info["total_params"]     = m_tot.group(1)
    if m_trn: info["trainable_params"] = m_trn.group(1)

    return info

# ── Cosine LR reconstruction for fine-tune phase ─────────────────────────────
def reconstruct_cosine_lr(n_epochs: int, steps_per_epoch: int = 4735,
                           initial_lr: float = 1e-5, alpha: float = 1e-7) -> np.ndarray:
    total_steps = n_epochs * steps_per_epoch
    t = np.linspace(0, total_steps - 1, n_epochs)
    return initial_lr * 0.5 * (1 + np.cos(np.pi * t / total_steps)) + alpha

# ── Theme helpers ─────────────────────────────────────────────────────────────
BG      = "#1a1d23"
AX_BG   = "#21252b"
BORDER  = "#2d313a"
TICK_C  = "#9da5b4"
WHITE   = "#ebebeb"
HT_C    = "#4fa3e0"    # phase-1 blue
FT_C    = "#e06c75"    # phase-2 red
TRAIN_C = "#56b6c2"    # teal
VAL_C   = "#e5c07b"    # amber
GREEN   = "#98c379"
PURPLE  = "#c678dd"

def _style(ax, title: str, legend: bool = True):
    ax.set_facecolor(AX_BG)
    ax.set_title(title, color=WHITE, fontsize=10, fontweight="bold", pad=8)
    ax.tick_params(colors=TICK_C, labelsize=8)
    ax.xaxis.label.set_color(TICK_C)
    ax.yaxis.label.set_color(TICK_C)
    for sp in ax.spines.values():
        sp.set_edgecolor(BORDER)
    if legend:
        leg = ax.legend(framealpha=0.25, labelcolor=WHITE,
                        facecolor="#2c313c", fontsize=8, edgecolor=BORDER)
        if leg:
            leg.get_frame().set_linewidth(0.5)

def _phase_bg(ax, n_ht: int, epochs_ht, epochs_ft):
    if epochs_ht:
        ax.axvspan(epochs_ht[0] - 0.5, epochs_ht[-1] + 0.5, alpha=0.07, color=HT_C, zorder=0)
    if epochs_ft:
        ax.axvspan(epochs_ft[0] - 0.5, epochs_ft[-1] + 0.5, alpha=0.07, color=FT_C, zorder=0)
        ax.axvline(n_ht + 0.5, color=TICK_C, lw=0.8, ls="--", alpha=0.45, zorder=1)

def _bar_values(ax, bars, fmt="{:.2f}", color=WHITE, fontsize=8, pad=0.004):
    for bar in bars:
        w = bar.get_width()
        ax.text(w + pad, bar.get_y() + bar.get_height() / 2,
                fmt.format(w), va="center", color=color, fontsize=fontsize)

# ── Main plot ─────────────────────────────────────────────────────────────────
def build_dashboard(ht: pd.DataFrame, ft: pd.DataFrame,
                    info: dict, out_path: Path, dpi: int, show: bool):

    has_ft   = not ft.empty
    n_ht     = len(ht)
    n_ft     = len(ft) if has_ft else 0

    epochs_ht  = ht["epoch_global"].tolist()
    epochs_ft  = ft["epoch_global"].tolist() if has_ft else []
    epochs_all = epochs_ht + epochs_ft

    # Merge metric arrays
    def _col(df, c):  return df[c].tolist() if c in df.columns else []

    acc_tr  = _col(ht, "accuracy")      + _col(ft, "accuracy")
    acc_val = _col(ht, "val_accuracy")  + _col(ft, "val_accuracy")
    los_tr  = _col(ht, "loss")          + _col(ft, "loss")
    los_val = _col(ht, "val_loss")      + _col(ft, "val_loss")
    t5_tr   = _col(ht, "top5_acc")      + _col(ft, "top5_acc")
    t5_val  = _col(ht, "val_top5_acc")  + _col(ft, "val_top5_acc")

    # Learning rate
    lr_ht  = _col(ht, "learning_rate")
    lr_ft  = reconstruct_cosine_lr(n_ft).tolist() if has_ft else []
    lr_all = lr_ht + lr_ft

    # Derived
    gap     = [tr - va for tr, va in zip(acc_tr, acc_val)]
    deltas  = [0.0] + [acc_val[i] - acc_val[i-1] for i in range(1, len(acc_val))]
    best_val_idx = int(np.argmax(acc_val))
    best_val     = acc_val[best_val_idx]

    # ── Figure layout ──────────────────────────────────────────────────────────
    n_rows = 5 if has_ft else 4      # extra row for FT-phase LR detail if present
    fig = plt.figure(figsize=(20, 26))
    fig.patch.set_facecolor(BG)
    gs  = GridSpec(5, 3, figure=fig, hspace=0.50, wspace=0.35,
                   top=0.955, bottom=0.04, left=0.07, right=0.97)

    def _ax(r, c, cs=1, rs=1):
        return fig.add_subplot(gs[r:r+rs, c:c+cs])

    # ── ROW 0: Accuracy (wide) + Loss ─────────────────────────────────────────
    ax_acc = _ax(0, 0, cs=2)
    ax_acc.plot(epochs_all, acc_tr,  color=TRAIN_C, lw=2.0, label="Train Top-1")
    ax_acc.plot(epochs_all, acc_val, color=VAL_C,   lw=2.0, label="Val   Top-1")
    if t5_tr:
        ax_acc.plot(epochs_all, t5_tr,  color=TRAIN_C, lw=1.2, ls="--", alpha=0.65, label="Train Top-5")
        ax_acc.plot(epochs_all, t5_val, color=VAL_C,   lw=1.2, ls="--", alpha=0.65, label="Val   Top-5")
    # Best val marker
    ax_acc.scatter([epochs_all[best_val_idx]], [best_val],
                   color=GREEN, s=80, zorder=5, label=f"Best val {best_val*100:.1f}%")
    ax_acc.set_xlabel("Epoch"); ax_acc.set_ylabel("Accuracy")
    _style(ax_acc, "Accuracy Over Time  (solid = Top-1 │ dashed = Top-5)")
    _phase_bg(ax_acc, n_ht, epochs_ht, epochs_ft)
    # Phase text
    if epochs_ht:
        ax_acc.text(np.mean(epochs_ht), ax_acc.get_ylim()[0] + 0.01,
                    "Phase 1 – Head", color=HT_C, fontsize=8, ha="center", alpha=0.8)
    if epochs_ft:
        ax_acc.text(np.mean(epochs_ft), ax_acc.get_ylim()[0] + 0.01,
                    "Phase 2 – Fine-tune", color=FT_C, fontsize=8, ha="center", alpha=0.8)

    ax_loss = _ax(0, 2)
    ax_loss.plot(epochs_all, los_tr,  color=TRAIN_C, lw=2.0, label="Train")
    ax_loss.plot(epochs_all, los_val, color=VAL_C,   lw=2.0, label="Val")
    ax_loss.set_xlabel("Epoch"); ax_loss.set_ylabel("Loss (label-smoothed CE)")
    _style(ax_loss, "Loss Over Time")
    _phase_bg(ax_loss, n_ht, epochs_ht, epochs_ft)

    # ── ROW 1: LR | Overfitting Gap | ΔVal ────────────────────────────────────
    ax_lr = _ax(1, 0)
    ax_lr.plot(epochs_all, lr_all, color=PURPLE, lw=2.0)
    ax_lr.set_yscale("log")
    ax_lr.set_xlabel("Epoch"); ax_lr.set_ylabel("LR (log scale)")
    _style(ax_lr, "Learning Rate Schedule", legend=False)
    _phase_bg(ax_lr, n_ht, epochs_ht, epochs_ft)
    if epochs_ht:
        ax_lr.text(np.mean(epochs_ht),   max(lr_all) * 0.55, "ReduceLROnPlateau",
                   color=HT_C, fontsize=7.5, ha="center")
    if epochs_ft:
        ax_lr.text(np.mean(epochs_ft), max(lr_all) * 0.55, "Cosine Decay\n(reconstructed)",
                   color=FT_C, fontsize=7.5, ha="center")

    ax_gap = _ax(1, 1)
    bar_colors_gap = [FT_C if e > n_ht else HT_C for e in epochs_all]
    ax_gap.bar(epochs_all, gap, color=bar_colors_gap, alpha=0.82, width=0.8)
    ax_gap.axhline(0, color=TICK_C, lw=0.8, ls="--")
    ax_gap.set_xlabel("Epoch"); ax_gap.set_ylabel("Train − Val Accuracy")
    _style(ax_gap, "Overfitting Gap", legend=False)
    ax_gap.legend(handles=[mpatches.Patch(color=HT_C, label="Phase 1"),
                            mpatches.Patch(color=FT_C, label="Phase 2")],
                  framealpha=0.25, labelcolor=WHITE, facecolor="#2c313c",
                  fontsize=8, edgecolor=BORDER)
    _phase_bg(ax_gap, n_ht, epochs_ht, epochs_ft)

    ax_delta = _ax(1, 2)
    delta_colors = [GREEN if d >= 0 else FT_C for d in deltas]
    ax_delta.bar(epochs_all, deltas, color=delta_colors, alpha=0.85, width=0.8)
    ax_delta.axhline(0, color=TICK_C, lw=0.8, ls="--")
    ax_delta.set_xlabel("Epoch"); ax_delta.set_ylabel("ΔVal Top-1")
    _style(ax_delta, "Val Accuracy Δ per Epoch", legend=False)
    _phase_bg(ax_delta, n_ht, epochs_ht, epochs_ft)

    # ── ROW 2: Top-5 Val comparison bar (both phases side by side) ─────────────
    ax_t5 = _ax(2, 0, cs=2)
    if t5_val:
        ax_t5.plot(epochs_all, t5_val, color=VAL_C,   lw=2.0, label="Val Top-5")
        ax_t5.plot(epochs_all, acc_val, color=TRAIN_C, lw=2.0, label="Val Top-1")
        ax_t5.fill_between(epochs_all, acc_val, t5_val,
                           color=PURPLE, alpha=0.12, label="Top-5 − Top-1 gap")
        ax_t5.set_xlabel("Epoch"); ax_t5.set_ylabel("Accuracy")
        _style(ax_t5, "Val Top-1 vs Top-5 Accuracy (shaded gap)")
        _phase_bg(ax_t5, n_ht, epochs_ht, epochs_ft)
    else:
        ax_t5.axis("off")
        ax_t5.text(0.5, 0.5, "No Top-5 data", color=TICK_C,
                   ha="center", va="center", transform=ax_t5.transAxes)

    # ── ROW 2: Model summary card ──────────────────────────────────────────────
    ax_info = _ax(2, 2)
    ax_info.set_facecolor(AX_BG)
    ax_info.axis("off")
    for sp in ax_info.spines.values():
        sp.set_edgecolor(BORDER)
    lines = [
        ("Model",          info.get("model_name", "?")),
        ("Date",           info.get("date", "?")),
        ("Classes",        info.get("n_classes", "?")),
        ("Image size",     info.get("img_size", "?")),
        ("Batch size",     info.get("batch_size", "?")),
        ("Head epochs",    info.get("epochs", "?")),
        ("FT epochs",      info.get("fine_tune_epochs", "?")),
        ("Optimizer",      info.get("optimizer", "Adam")),
        ("Init LR",        info.get("lr", "?")),
        ("Label smooth",   info.get("label_smoothing", "?")),
        ("Total params",   info.get("total_params", "?")),
        ("Trainable",      info.get("trainable_params", "?")),
        ("Best val acc",   f"{best_val*100:.2f}%  (ep {epochs_all[best_val_idx]})"),
        ("Final val acc",  f"{acc_val[-1]*100:.2f}%"),
    ]
    y = 0.97
    ax_info.set_title("Model Config", color=WHITE, fontsize=10, fontweight="bold", pad=8)
    for label, val in lines:
        ax_info.text(0.04, y, f"{label}:", color=TICK_C, fontsize=8,
                     transform=ax_info.transAxes, va="top")
        ax_info.text(0.48, y, str(val), color=WHITE, fontsize=8,
                     transform=ax_info.transAxes, va="top")
        y -= 0.067

    # ── ROW 3: Per-epoch seconds (if available) + Val loss min marker ──────────
    # Smooth val loss with rolling min to show convergence
    ax_conv = _ax(3, 0, cs=2)
    val_loss_arr = np.array(los_val)
    running_min  = np.minimum.accumulate(val_loss_arr)
    ax_conv.plot(epochs_all, los_val,    color=VAL_C,   lw=2.0, label="Val Loss", alpha=0.9)
    ax_conv.plot(epochs_all, running_min, color=GREEN,  lw=1.4, ls="--", label="Running Min Val Loss")
    ax_conv.scatter([epochs_all[int(np.argmin(los_val))]],
                    [min(los_val)], color=GREEN, s=80, zorder=5)
    ax_conv.set_xlabel("Epoch"); ax_conv.set_ylabel("Val Loss")
    _style(ax_conv, "Val Loss — Convergence Trace (dashed = running minimum)")
    _phase_bg(ax_conv, n_ht, epochs_ht, epochs_ft)

    # ── ROW 3: LR events annotation ───────────────────────────────────────────
    ax_lr2 = _ax(3, 2)
    lr_arr = np.array(lr_ht)
    # Detect reduction events (where LR dropped by >10%)
    lr_events = [i for i in range(1, len(lr_arr)) if lr_arr[i] < lr_arr[i-1] * 0.95]
    ax_lr2.step(epochs_ht, lr_ht, color=PURPLE, lw=2.0, where="post", label="LR (Phase 1)")
    for ev in lr_events:
        ax_lr2.axvline(epochs_ht[ev], color=FT_C, lw=1.0, ls=":", alpha=0.8)
        ax_lr2.text(epochs_ht[ev] + 0.2, lr_arr[ev] * 1.05,
                    f"↓ ep{epochs_ht[ev]}", color=FT_C, fontsize=7.5)
    ax_lr2.set_yscale("log")
    ax_lr2.set_xlabel("Epoch"); ax_lr2.set_ylabel("LR (log)")
    _style(ax_lr2, "Phase 1 LR — ReduceLROnPlateau events")
    _phase_bg(ax_lr2, n_ht, epochs_ht, [])

    # ── ROW 4: Accuracy gain per phase summary (horizontal bar comparison) ─────
    ax_phase = _ax(4, 0)
    categories = ["Train Top-1", "Val Top-1", "Train Top-5", "Val Top-5"]
    ht_end = [
        acc_tr[n_ht - 1]  * 100,
        acc_val[n_ht - 1] * 100,
        (t5_tr[n_ht - 1]  * 100 if t5_tr else 0),
        (t5_val[n_ht - 1] * 100 if t5_val else 0),
    ]
    ft_end = [
        acc_tr[-1]  * 100,
        acc_val[-1] * 100,
        (t5_tr[-1]  * 100 if t5_tr else 0),
        (t5_val[-1] * 100 if t5_val else 0),
    ] if has_ft else ht_end

    y_pos = np.arange(len(categories))
    ax_phase.barh(y_pos - 0.2, ht_end, height=0.35, color=HT_C, alpha=0.85, label="After Phase 1")
    ax_phase.barh(y_pos + 0.2, ft_end, height=0.35, color=FT_C, alpha=0.85, label="After Phase 2")
    ax_phase.set_yticks(y_pos); ax_phase.set_yticklabels(categories, color=WHITE, fontsize=9)
    ax_phase.set_xlabel("Accuracy (%)")
    _style(ax_phase, "Phase 1 vs Phase 2 — Final Accuracy Comparison")
    for i, (h, f) in enumerate(zip(ht_end, ft_end)):
        ax_phase.text(h + 0.3, i - 0.2, f"{h:.1f}%", va="center", color=WHITE, fontsize=8)
        ax_phase.text(f + 0.3, i + 0.2, f"{f:.1f}%", va="center", color=WHITE, fontsize=8)

    # ── ROW 4: Gain from fine-tuning per metric ────────────────────────────────
    ax_gain = _ax(4, 1)
    if has_ft:
        gain_labels = ["Val Top-1", "Val Top-5", "Train Top-1"]
        gains = [
            (acc_val[-1] - acc_val[n_ht - 1]) * 100,
            ((t5_val[-1] - t5_val[n_ht - 1]) * 100 if t5_val else 0),
            (acc_tr[-1]  - acc_tr[n_ht - 1])  * 100,
        ]
        colors_g = [GREEN if g >= 0 else FT_C for g in gains]
        bars_g   = ax_gain.barh(gain_labels, gains, color=colors_g, alpha=0.85)
        ax_gain.axvline(0, color=TICK_C, lw=0.8, ls="--")
        ax_gain.set_xlabel("Δ Accuracy (pp)")
        _style(ax_gain, "Gain from Fine-tuning (Phase 2 − Phase 1 end)", legend=False)
        for bar, g in zip(bars_g, gains):
            xpos = bar.get_width()
            ax_gain.text(xpos + (0.1 if xpos >= 0 else -0.1), bar.get_y() + bar.get_height()/2,
                         f"{g:+.2f}pp", va="center",
                         ha="left" if xpos >= 0 else "right",
                         color=WHITE, fontsize=8)
    else:
        ax_gain.axis("off")
        ax_gain.text(0.5, 0.5, "Fine-tune log not available",
                     color=TICK_C, ha="center", va="center", transform=ax_gain.transAxes)

    # ── ROW 4: Training stability (std of loss in rolling window) ─────────────
    ax_stab = _ax(4, 2)
    window = 3
    if len(los_tr) >= window:
        rolling_std_tr  = pd.Series(los_tr).rolling(window, center=True).std().fillna(0)
        rolling_std_val = pd.Series(los_val).rolling(window, center=True).std().fillna(0)
        ax_stab.fill_between(epochs_all, rolling_std_tr,  color=TRAIN_C, alpha=0.5, label="Train")
        ax_stab.fill_between(epochs_all, rolling_std_val, color=VAL_C,   alpha=0.5, label="Val")
        ax_stab.set_xlabel("Epoch"); ax_stab.set_ylabel(f"Loss StdDev (w={window})")
        _style(ax_stab, "Training Stability — Rolling Loss Std Dev")
        _phase_bg(ax_stab, n_ht, epochs_ht, epochs_ft)
    else:
        ax_stab.axis("off")

    # ── Suptitle ───────────────────────────────────────────────────────────────
    subtitle = (
        f"{info.get('model_name','?')} — Training Dashboard  "
        f"│  Best Val Top-1 {best_val*100:.2f}%  "
        f"│  Final Val Top-1 {acc_val[-1]*100:.2f}%  "
        f"│  {n_ht + n_ft} total epochs"
    )
    fig.suptitle(subtitle, color=WHITE, fontsize=13, fontweight="bold")

    # ── Save ───────────────────────────────────────────────────────────────────
    plt.savefig(out_path, dpi=dpi, bbox_inches="tight", facecolor=BG)
    print(f"[OK] Dashboard saved → {out_path}")

    if show:
        matplotlib.use("TkAgg")
        plt.show()
    plt.close(fig)

# ── Entry point ───────────────────────────────────────────────────────────────
def main():
    args = parse_args()
    model_dir, model_name = resolve_model_dir(args.base_dir, args.model_name)

    print(f"[INFO] Loading logs from: {model_dir}")
    ht, ft   = load_logs(model_dir, model_name)
    info     = load_model_info(model_dir, model_name)

    out_path = Path(args.out) if args.out else model_dir / "training_dashboard.png"

    if args.show:
        import importlib
        matplotlib.use("TkAgg")

    build_dashboard(ht, ft, info, out_path, dpi=args.dpi, show=args.show)

if __name__ == "__main__":
    main()
