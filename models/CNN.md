---
jupyter:
  jupytext:
    text_representation:
      extension: .md
      format_name: markdown
      format_version: '1.3'
      jupytext_version: 1.18.1
  kernelspec:
    display_name: Python 3 (ipykernel)
    language: python
    name: python3
---

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 119979, "status": "ok", "timestamp": 1774834475689, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="ZYWiJL5CQMku" outputId="fbe4e18d-e0fb-4f9d-b6a8-3adcf5d5a566"
# !mkdir /food
# !cd food
# !wget http://data.vision.ee.ethz.ch/cvl/food-101.tar.gz
# !tar xzvf food-101.tar.gz
```

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 123, "status": "ok", "timestamp": 1774834475848, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="ct2dbU_0MlLC" outputId="d1829955-cee6-4c0d-8bb0-ef55272fe4e3"
!nvidia-smi
```

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 122, "status": "ok", "timestamp": 1774834475954, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="a5eATZjSS-4m" outputId="0b17bac9-c3b1-407f-ec76-a4ff0bcaa3e6"
!ls
```

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 5849, "status": "ok", "timestamp": 1774834481794, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="hZE6I9ozSciE" outputId="772ff2f7-b19d-49b0-d7de-77db022b6cda"
!pip install tensorflow keras split-folders opencv-python
```

```python id="0ixjkG3nQYTa"
import os
import shutil
import stat
import seaborn as sns
import collections
import h5py
import numpy as np
import tensorflow as tf
import matplotlib.image as img
import random
import cv2
import PIL
import matplotlib.pyplot as plt
import matplotlib.image as img
from os import listdir
from os.path import isfile, join
from collections import defaultdict
from ipywidgets import interact, interactive, fixed
import ipywidgets as widgets
from sklearn.model_selection import train_test_split
import keras
from skimage.io import imread
#from keras.utils.np_utils import to_categorical
from keras.applications.inception_v3 import preprocess_input
from keras.models import load_model
from shutil import copy
from shutil import copytree, rmtree
import tensorflow as tf
# import tensorflow.keras.backen
from tensorflow.keras.applications import InceptionV3
from tensorflow.keras.applications.inception_v3 import preprocess_input
from tensorflow.keras import layers, Model, regularizers
from tensorflow.keras.callbacks import ModelCheckpoint, CSVLogger, EarlyStopping, ReduceLROnPlateau


```

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 11, "status": "ok", "timestamp": 1774834510876, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="tkvu4YMwR8c9" outputId="dbaa3dad-3f23-4251-86fb-c88605a28a9a"
class_N = {}
N_class = {}
with open('food-101/meta/classes.txt', 'r') as txt:
    classes = [i.strip() for i in txt.readlines()]
    class_N = dict(zip(classes, range(len(classes))))
    N_class = dict(zip(range(len(classes)), classes))
    class_N = {i: j for j, i in N_class.items()}
class_N_sorted = collections.OrderedDict(sorted(class_N.items()))
print(class_N)

# Method to generate directory-file map.
def gen_dir_file_map(path):
    dir_files = defaultdict(list)
    with open(path, 'r') as txt:
        files = [i.strip() for i in txt.readlines()]
        for f in files:
            dir_name, id = f.split('/')
            dir_files[dir_name].append(id + '.jpg')
    return dir_files

# Method to recursively copy a directory.
def copytree(source, target, symlinks = False, ignore = None):
  if not os.path.exists(target):
      os.makedirs(target)
      shutil.copystat(source, target)
  data = os.listdir(source)
  if ignore:
      exclude = ignore(source, data)
      data = [x for x in data if x not in exclude]
  for item in data:
      src = os.path.join(source, item)
      dest = os.path.join(target, item)
      if symlinks and os.path.islink(src):
          if os.path.lexists(dest):
              os.remove(dest)
          os.symlink(os.readlink(src), dest)
          try:
              st = os.lstat(src)
              mode = stat.S_IMODE(st.st_mode)
              os.lchmod(dest, mode)
          except:
              pass
      elif os.path.isdir(src):
          copytree(src, dest, symlinks, ignore)
      else:
          shutil.copy2(src, dest)

# Train files to ignore.
def ignore_train(d, filenames):
  subdir = d.split('/')[-1]
  train_dir_files = gen_dir_file_map('food-101/meta/train.txt')
  to_ignore = train_dir_files[subdir]
  return to_ignore

# Test files to ignore.
def ignore_test(d, filenames):
  subdir = d.split('/')[-1]
  test_dir_files = gen_dir_file_map('food-101/meta/test.txt')
  to_ignore = test_dir_files[subdir]
  return to_ignore

# Method to load and resize images.
def load_images(path_to_imgs):
  resize_count = 0

  invalid_count = 0
  all_imgs = []
  all_classes = []

  for i, subdir in enumerate(listdir(path_to_imgs)):
      imgs = listdir(join(path_to_imgs, subdir))
      classN = class_N[subdir]
      for img_name in imgs:
          img_arr = cv2.imread(join(path_to_imgs, subdir, img_name))
          img_arr_rs = img_arr
          img_arr_rs = cv2.resize(img_arr, (200,200),interpolation=cv2.INTER_AREA)
          resize_count += 1
          im_rgb = cv2.cvtColor(img_arr_rs, cv2.COLOR_BGR2RGB)
          all_imgs.append(im_rgb)
          all_classes.append(classN)

  return np.array(all_imgs), np.array(all_classes)

# Method to generate train-test files.
def gen_train_test_split(path_to_imgs = 'food-101/images' , target_path = 'food-101'):
  copytree(path_to_imgs, target_path + '/train', ignore=ignore_test)
  copytree(path_to_imgs, target_path + '/test', ignore=ignore_train)

# Method to load train-test files.
def load_test_data(path_to_test_imgs):
  X_test, y_test = load_images(path_to_test_imgs)
  return X_test, y_test

def load_train_data(path_to_train_imgs):
  X_train, y_train = load_images(path_to_train_imgs)
  return X_train, y_train,


```

```python id="1qY6tbG8Qbp5"
IMG_SIZE    = (299, 299)   # InceptionV3's native size (better than 200x200)
BATCH_SIZE  = 8
N_CLASSES   = 101
TRAIN_DIR   = '/data/food-101/train'
TEST_DIR    = '/data/food-101/test'
AUTOTUNE    = tf.data.AUTOTUNE

```

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 131659, "status": "ok", "timestamp": 1774834642562, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="-MvG3AlgV20l" outputId="e5450987-cd42-40bf-a6f0-251bba5a964d"
# Generate train-test files.
if not os.path.isdir('./food-101/test') and not os.path.isdir('./food-101/train'):
    gen_train_test_split()
    len_train = len(os.listdir('/content/food-101/train'))
    len_test = len(os.listdir('/content/food-101/test')) # Fixed path from food-100 to food-101
    print(len_train,len_test)
else:
    print('train and test folders already exists.')
    len_train = len(os.listdir('/content/food-101/train'))
    len_test = len(os.listdir('/content/food-101/test'))
    print(len_train,len_test)
```

```python colab={"base_uri": "https://localhost:8080/"} executionInfo={"elapsed": 5857, "status": "ok", "timestamp": 1774834648398, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="SKQvIKVjQc1j" outputId="c58cffc1-c5b8-4bd3-d5e6-c077ca2cc6ab"
@tf.function
def augment(image, label):
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_brightness(image, max_delta=0.2)
    image = tf.image.random_contrast(image, lower=0.8, upper=1.2)
    image = tf.image.random_saturation(image, lower=0.8, upper=1.2)
    # Pad 10% then random crop back to IMG_SIZE (replaces RandomZoom/Translate)
    pad_h = int(IMG_SIZE[0] * 0.1)
    pad_w = int(IMG_SIZE[1] * 0.1)
    image = tf.image.pad_to_bounding_box(
        image, pad_h, pad_w,
        IMG_SIZE[0] + 2 * pad_h,
        IMG_SIZE[1] + 2 * pad_w
    )
    # Fix: Include batch dimension in random_crop size
    image = tf.image.random_crop(image, size=[tf.shape(image)[0], IMG_SIZE[0], IMG_SIZE[1], 3])
    return image, label

def build_dataset(directory, augment_data=False):
    ds = tf.keras.utils.image_dataset_from_directory(
        directory,
        image_size=IMG_SIZE,
        batch_size=BATCH_SIZE,
        label_mode='categorical'
    )
    if augment_data:
        ds = ds.map(augment, num_parallel_calls=AUTOTUNE)
    # preprocess_input: scales [0,255] → [-1, 1] as InceptionV3 expects
    ds = ds.map(lambda x, y: (preprocess_input(x), y),
                num_parallel_calls=AUTOTUNE)

    # Added caching to a file
    # ds = ds.cache(f'/content/cache_{"train" if augment_data else "test"}')
    return ds.prefetch(AUTOTUNE)

train_ds = build_dataset(TRAIN_DIR, augment_data=True)
test_ds  = build_dataset(TEST_DIR,  augment_data=False)
```

<!-- #region id="N0ruLWQYC3-0" -->

<!-- #endregion -->

```python colab={"base_uri": "https://localhost:8080/", "height": 806} id="JipQUmIFQk9r" outputId="8968a5f2-872b-4987-c4b2-101434c36ecc"
# TRAINING
base_model = InceptionV3(weights='imagenet', include_top=False,
                         input_shape=(*IMG_SIZE, 3))
base_model.trainable = False

inputs = keras.Input(shape=(*IMG_SIZE, 3))
# training=False: keeps InceptionV3's BatchNorm layers in inference mode
# while frozen — prevents corrupting pretrained running statistics
x = base_model(inputs, training=False)
x = layers.GlobalAveragePooling2D()(x)
x = layers.Dense(256, activation='relu')(x)
x = layers.Dropout(0.3)(x)
outputs = layers.Dense(N_CLASSES,
                       kernel_regularizer=regularizers.l2(0.005),
                       activation='softmax')(x)

model = Model(inputs, outputs)
model.compile(
    optimizer=keras.optimizers.Adam(learning_rate=1e-3),
    loss='categorical_crossentropy',
    metrics=['accuracy']
)
model.summary(show_trainable=True)

print("\n=== Phase 1: Head training ===")
history_p1 = model.fit(
    train_ds, validation_data=test_ds, epochs=30,
    callbacks=[
        ModelCheckpoint('best_model_phase1.keras',
                        save_best_only=True, monitor='val_accuracy'),
        EarlyStopping(patience=5, restore_best_weights=True),
        CSVLogger('history_phase1.log'),
    ]
)
```

```python colab={"base_uri": "https://localhost:8080/", "height": 436} executionInfo={"elapsed": 23045, "status": "error", "timestamp": 1774877180924, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}, "user_tz": -60} id="XEGh8GxYQtCt" outputId="882033b3-28d0-4f61-a1cb-b8150819c9b4"
# ── Phase 2: Fine-tuning ────────────────────────────────────────────────────
# Resilient to kernel restarts: reload model + re-extract base_model if needed
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras.applications.inception_v3 import InceptionV3
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping, ReduceLROnPlateau, CSVLogger

try:
    base_model  # already in scope from Cell 10
except NameError:
    print("base_model not in scope — reloading from best_model_phase1.keras")
    model = keras.models.load_model('best_model_phase1.keras')
    # InceptionV3 is always the first layer of the functional model
    base_model = next(l for l in model.layers if isinstance(l, tf.keras.Model))

# Unfreeze top 50 layers, keep the rest frozen
base_model.trainable = True
for layer in base_model.layers[:-50]:
    layer.trainable = False

FINE_TUNE_EPOCHS = 30
steps_per_epoch  = len(train_ds)
total_steps      = FINE_TUNE_EPOCHS * steps_per_epoch

# Cosine decay: smooth LR anneal over all epochs
lr_schedule = tf.keras.optimizers.schedules.CosineDecay(
    initial_learning_rate=1e-4,
    decay_steps=total_steps,
    alpha=1e-6
)

model.compile(
    optimizer=tf.keras.optimizers.SGD(learning_rate=lr_schedule, momentum=0.9),
    loss='categorical_crossentropy',
    metrics=['accuracy', tf.keras.metrics.TopKCategoricalAccuracy(k=5, name='top5_accuracy')]
)

# MixUp augmentation
@tf.function
def mixup(ds_one, ds_two):
    images_one, labels_one = ds_one
    images_two, labels_two = ds_two
    lam = tf.random.uniform([tf.shape(images_one)[0], 1, 1, 1], 0.2, 0.8)
    images = lam * images_one + (1.0 - lam) * images_two
    labels = lam[:, 0, 0, :1] * tf.cast(labels_one, tf.float32) \
           + (1.0 - lam[:, 0, 0, :1]) * tf.cast(labels_two, tf.float32)
    return images, labels

train_ds_mixed = (
    tf.data.Dataset.zip((
        train_ds.shuffle(1000, reshuffle_each_iteration=True),
        train_ds.shuffle(1000, reshuffle_each_iteration=True),
    ))
    .map(mixup, num_parallel_calls=tf.data.AUTOTUNE)
    .prefetch(tf.data.AUTOTUNE)
)

callbacks_ft = [
    ModelCheckpoint('best_model_finetuned.keras', save_best_only=True, monitor='val_accuracy'),
    EarlyStopping(patience=7, restore_best_weights=True),
    ReduceLROnPlateau(monitor='val_loss', factor=0.3, patience=3, min_lr=1e-7),
    CSVLogger('history_finetune.log'),
]

print("\n=== Phase 2: Fine-tuning (top-50 layers unlocked) ===")
history_ft = model.fit(
    train_ds_mixed,
    validation_data=test_ds,
    epochs=FINE_TUNE_EPOCHS,
    callbacks=callbacks_ft
)
model.save('food101_inceptionv3_final.keras')

```

```python id="yFkhl_gtF9Ht"
# ── Validation on TEST_DIR ──────────────────────────────────────────────────
import numpy as np
import matplotlib.pyplot as plt
from sklearn.metrics import classification_report, confusion_matrix
import seaborn as sns

# Load best fine-tuned model (resilient to kernel restart)
try:
    model
except NameError:
    print("Loading best_model_finetuned.keras ...")
    model = keras.models.load_model('best_model_finetuned.keras')

# Rebuild test_ds if needed
try:
    test_ds
except NameError:
    test_ds = build_dataset(TEST_DIR, augment_data=False)

# ── Collect predictions ──────────────────────────────────────────────────────
print("Running inference on test set ...")
y_true, y_pred = [], []

for images, labels in test_ds:
    preds = model.predict(images, verbose=0)
    y_true.extend(np.argmax(labels.numpy(), axis=1))
    y_pred.extend(np.argmax(preds, axis=1))

y_true = np.array(y_true)
y_pred = np.array(y_pred)

# ── Metrics ──────────────────────────────────────────────────────────────────
top1_acc = np.mean(y_true == y_pred)
# Top-5: check if true label is in top-5 predicted indices
y_pred_top5 = []
for images, labels in test_ds:
    preds = model.predict(images, verbose=0)
    top5 = np.argsort(preds, axis=1)[:, -5:]
    y_pred_top5.extend(top5.tolist())

true_in_top5 = [y_true[i] in y_pred_top5[i] for i in range(len(y_true))]
top5_acc = np.mean(true_in_top5)

print(f"\n{'='*40}")
print(f"  Top-1 Accuracy : {top1_acc*100:.2f}%")
print(f"  Top-5 Accuracy : {top5_acc*100:.2f}%")
print(f"{'='*40}\n")

# ── Per-class report (top 20 worst classes) ──────────────────────────────────
class_names = [N_class[i] for i in range(N_CLASSES)]
report = classification_report(y_true, y_pred, target_names=class_names, output_dict=True)

per_class_f1 = {cls: report[cls]['f1-score'] for cls in class_names}
worst20 = sorted(per_class_f1.items(), key=lambda x: x[1])[:20]

print("20 worst-performing classes (by F1):")
for cls, f1 in worst20:
    print(f"  {cls:<30} F1={f1:.3f}")

# ── Confusion matrix (top-20 worst classes only, for readability) ─────────────
worst_indices = [class_names.index(c) for c, _ in worst20]
mask_true = np.isin(y_true, worst_indices)
cm = confusion_matrix(y_true[mask_true], y_pred[mask_true], labels=worst_indices)

plt.figure(figsize=(14, 12))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=[class_names[i] for i in worst_indices],
            yticklabels=[class_names[i] for i in worst_indices])
plt.title('Confusion Matrix — 20 Worst Classes')
plt.ylabel('True label')
plt.xlabel('Predicted label')
plt.xticks(rotation=45, ha='right')
plt.tight_layout()
plt.show()

```

```python id="1pIPDfQpG6AC"
# Clear CACHE
# import subprocess
# result = subprocess.run(
#     'rm -rf /content/*.tempstate* /content/*.lockfile /content/food-101.tar.gz && df -h /content',
#     shell=True, capture_output=True, text=True
# )
# print(result.stdout)
# print(result.stderr)
```

```python id="tA61bZLhGXR_"
# # CLEAN DEAD FILES
# import subprocess

# # Show the biggest directories eating your disk
# result = subprocess.run(
#     ['du', '-sh', '--threshold=100M',
#      '/content/cache_train',
#      '/content/cache_test',
#      '/content/food-101',
#      '/content/drive/.shortcut-targets-by-id',
#      '/root/.keras',
#      '/tmp'],
#     capture_output=True, text=True
# )
# print(result.stdout)
# print(result.stderr)

# # Also show overall breakdown
# print("\n--- Top space consumers in /content ---")
# subprocess.run(['du', '-sh', '/content/*'], shell=False)
# result2 = subprocess.run('du -sh /content/* 2>/dev/null | sort -rh | head -20',
#                          shell=True, capture_output=True, text=True)
# print(result2.stdout)
```
