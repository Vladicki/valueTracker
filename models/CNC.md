---
jupyter:
  jupytext:
    text_representation:
      extension: .md
      format_name: markdown
      format_version: '1.3'
      jupytext_version: 1.18.1
  kernelspec:
    display_name: Python 3
    name: python3
---

```python colab={"base_uri": "https://localhost:8080/"} id="ZYWiJL5CQMku" executionInfo={"status": "ok", "timestamp": 1774580118389, "user_tz": 0, "elapsed": 310681, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="4623ea19-b476-413e-f338-7c37317e1dc9"
!wget http://data.vision.ee.ethz.ch/cvl/food-101.tar.gz
!tar xzvf food-101.tar.gz
```

```python colab={"base_uri": "https://localhost:8080/"} id="ct2dbU_0MlLC" executionInfo={"status": "ok", "timestamp": 1774580118494, "user_tz": 0, "elapsed": 185, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="34a9b712-5bb6-4ae6-c31e-df7c60a93ea1"
!nvidia-smi
```

```python colab={"base_uri": "https://localhost:8080/"} id="a5eATZjSS-4m" executionInfo={"status": "ok", "timestamp": 1774580118710, "user_tz": 0, "elapsed": 207, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="2c90d8dd-cf49-4a4e-93d2-3c5ea85ca6d5"
!ls
```

```python colab={"base_uri": "https://localhost:8080/"} id="hZE6I9ozSciE" executionInfo={"status": "ok", "timestamp": 1774580124555, "user_tz": 0, "elapsed": 5865, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="991c240d-1fbf-4991-be75-08575ebe4f32"
!pip install tensorflow keras split-folders opencv-python
```

```python id="0ixjkG3nQYTa" executionInfo={"status": "ok", "timestamp": 1774580147286, "user_tz": 0, "elapsed": 22720, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}}
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

```python colab={"base_uri": "https://localhost:8080/"} id="tkvu4YMwR8c9" executionInfo={"status": "ok", "timestamp": 1774580147361, "user_tz": 0, "elapsed": 51, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="b086673b-d933-4747-ae15-4fcc59e232ae"
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

```python id="1qY6tbG8Qbp5" executionInfo={"status": "ok", "timestamp": 1774580147361, "user_tz": 0, "elapsed": 30, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}}
IMG_SIZE    = (299, 299)   # InceptionV3's native size (better than 200x200)
BATCH_SIZE  = 16
N_CLASSES   = 101
TRAIN_DIR   = '/content/food-101/train'
TEST_DIR    = '/content/food-101/test'
AUTOTUNE    = tf.data.AUTOTUNE

```

```python colab={"base_uri": "https://localhost:8080/"} id="-MvG3AlgV20l" executionInfo={"status": "ok", "timestamp": 1774580258669, "user_tz": 0, "elapsed": 111325, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="51af31df-d7b6-44a2-d67f-17010fc64bb7"
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

```python colab={"base_uri": "https://localhost:8080/"} id="SKQvIKVjQc1j" executionInfo={"status": "ok", "timestamp": 1774580263405, "user_tz": 0, "elapsed": 4725, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="6f479c9e-2b08-4acf-cae4-523275289f90"
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

```python id="JipQUmIFQk9r" colab={"base_uri": "https://localhost:8080/", "height": 943} executionInfo={"status": "ok", "timestamp": 1774589794190, "user_tz": 0, "elapsed": 9530759, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="492aa4f4-85fb-42cc-bae7-16ef5146e8b7"
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
    train_ds, validation_data=test_ds, epochs=15,
    callbacks=[
        ModelCheckpoint('best_model_phase1.keras',
                        save_best_only=True, monitor='val_accuracy'),
        EarlyStopping(patience=5, restore_best_weights=True),
        CSVLogger('history_phase1.log'),
    ]
)
```

```python id="XEGh8GxYQtCt" colab={"base_uri": "https://localhost:8080/", "height": 211} executionInfo={"status": "error", "timestamp": 1774589794263, "user_tz": 0, "elapsed": 52, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}} outputId="e29d0041-4fe4-4ffe-c125-1dab5e51a352"
# Unfreeze from the last 50 layers onward
base.trainable = True
fo r layer in base.layers[:-50]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.SGD(learning_rate=1e-4, momentum=0.9),
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

callbacks_ft = [
    ModelCheckpoint('best_model_finetuned.keras', save_best_only=True, monitor='val_accuracy'),
    EarlyStopping(patience=7, restore_best_weights=True),
    ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=3),
    CSVLogger('history_finetune.log'),
]

history_ft = model.fit(train_ds, validation_data=test_ds, epochs=30, callbacks=callbacks_ft)
model.save('food101_inceptionv3_final.keras')
```

```python id="1pIPDfQpG6AC" executionInfo={"status": "aborted", "timestamp": 1774589794291, "user_tz": 0, "elapsed": 13, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}}
# Clear CACHE
# import subprocess
# result = subprocess.run(
#     'rm -rf /content/*.tempstate* /content/*.lockfile /content/food-101.tar.gz && df -h /content',
#     shell=True, capture_output=True, text=True
# )
# print(result.stdout)
# print(result.stderr)
```

```python id="tA61bZLhGXR_" executionInfo={"status": "aborted", "timestamp": 1774589794307, "user_tz": 0, "elapsed": 6, "user": {"displayName": "Vladislav Iurev", "userId": "12296008151878369638"}}
import subprocess

# Show the biggest directories eating your disk
result = subprocess.run(
    ['du', '-sh', '--threshold=100M',
     '/content/cache_train',
     '/content/cache_test',
     '/content/food-101',
     '/content/drive/.shortcut-targets-by-id',
     '/root/.keras',
     '/tmp'],
    capture_output=True, text=True
)
print(result.stdout)
print(result.stderr)

# Also show overall breakdown
print("\n--- Top space consumers in /content ---")
subprocess.run(['du', '-sh', '/content/*'], shell=False)
result2 = subprocess.run('du -sh /content/* 2>/dev/null | sort -rh | head -20',
                         shell=True, capture_output=True, text=True)
print(result2.stdout)
```
