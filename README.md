# ValueTracker: Transfer Learning Food Classification for On-Device Nutrition Tracking

![Training curves](models/food_graphs.svg)

![Confusion heatmap](models/heatmap.svg)

ValueTracker is a machine learning focused Android project for food image recognition and nutrition tracking. The project combines a transfer-learned convolutional neural network trained on Food-101 with a Kotlin Android application that performs local TensorFlow Lite inference from camera or gallery images.

Primary ML goal: build, compare, and deploy a food classifier that can recognise 101 food categories from images and run inside a mobile nutrition workflow.

Primary app goal: make image capture, prediction, nutrition lookup, and meal logging work as one local-first user flow.

---

## Contents

- [System overview](#system-overview)
- [Machine learning focus](#machine-learning-focus)
- [Imported paper figures and experiment graphs](#imported-paper-figures-and-experiment-graphs)
- [Dataset](#dataset)
- [Model architecture](#model-architecture)
- [Training methodology](#training-methodology)
- [Experimental results](#experimental-results)
- [TensorFlow Lite deployment](#tensorflow-lite-deployment)
- [Android application](#android-application)
- [Repository structure](#repository-structure)
- [Setup](#setup)
- [Run and test](#run-and-test)
- [Known limitations](#known-limitations)
- [Future work](#future-work)

---

## System overview

```text
Food-101 images
      |
      v
TensorFlow input pipeline
      |
      v
Augmentation + InceptionV3 preprocessing
      |
      v
Transfer learning CNN
      |        Phase 1: frozen ImageNet backbone, train classifier head
      |        Phase 2: partial fine-tuning, lower learning rate
      v
Keras model checkpoint
      |
      v
TensorFlow Lite conversion
      |
      v
Kotlin Android app
      |        CameraX or gallery input
      |        TFLite inference
      |        Room/DataStore nutrition tracking
      v
Predicted food class + local meal history
```

The academic centre of the repository is the food classification pipeline. The Android application exists to test whether the trained classifier can be integrated into a practical, local, mobile nutrition-tracking workflow.

---

## Machine learning focus

This project treats food recognition as a fine-grained multi-class image classification problem. Food images are difficult because classes often share colour, texture, shape, and plating structure. The same class can also appear in many visual forms due to lighting, viewpoint, ingredients, cooking style, and presentation.

The model pipeline is built around transfer learning:

1. Start from ImageNet-pretrained CNN features.
2. Remove original ImageNet classification head.
3. Add custom Food-101 classification head.
4. Train head while backbone is frozen.
5. Fine-tune later convolutional layers with smaller learning rate.
6. Export best model to TensorFlow Lite for Android inference.

Core modelling assumptions:

- Early CNN layers learn generic visual primitives such as edges, colours, corners, blobs, and simple textures.
- Later layers encode more task-specific compositions.
- Freezing the backbone stabilises early optimisation.
- Partial unfreezing improves adaptation to Food-101 without fully destroying pretrained features.
- Top-5 accuracy is important because food classes can be semantically and visually close.

---

## Imported paper figures and experiment graphs

The README imports the main explanatory and experimental figures already present in the thesis/paper materials under `models/ML/docs/`.

### CNN feature extraction

CNNs learn hierarchical features by combining local convolutional filters, nonlinear activations, and downsampling operations. In this project, the pretrained backbone provides the majority of the feature extraction capacity, while the custom head maps the extracted representation to Food-101 classes.

### Convolution and stride

The classifier relies on convolutional feature extraction, where shared kernels scan across spatial regions. Strided operations and pooling reduce spatial resolution while increasing semantic abstraction.

### Max pooling

Pooling layers reduce sensitivity to small translations and compress intermediate feature maps. This supports robustness when food is photographed from different viewpoints or with slightly shifted framing.

### Batch normalization

Batch normalization is used in the custom head and is also present throughout InceptionV3. During transfer learning, frozen backbone BatchNorm behaviour must be handled carefully because updating running statistics on a new dataset can destabilise pretrained representations.

### Inception module

Inception-style blocks process visual information at multiple effective receptive fields. This is useful for food images because a single class can contain both small local details, such as grains or toppings, and larger global structures, such as circular pizzas or plated meals.

### Inception factorization

InceptionV3 uses factorised convolutions to reduce computation while preserving representational capacity. This matters for deployment because a model intended for mobile use must balance accuracy, size, and latency.

### InceptionV3 training curves

The retained InceptionV3 experiments show stable transfer-learning behaviour. Validation accuracy improves substantially during training, then begins to plateau, which motivates early stopping and lower learning-rate fine-tuning.

### InceptionV3 confusion heatmap

The confusion heatmap supports per-class error analysis. Food classification errors tend to cluster between visually or semantically related foods rather than being uniformly random.

### EfficientNetV2-S training curves

EfficientNetV2-S was retained as a stronger benchmark comparison. Its saved experiment artifacts show higher validation performance than the InceptionV3 family, although the Android application currently targets an InceptionV3 TFLite asset.

### EfficientNetV2-S confusion heatmap

The EfficientNetV2-S heatmap is useful for comparing whether architecture improvements reduce the same class-confusion patterns or merely improve aggregate accuracy.

---

## Dataset

The training pipeline is based on Food-101.

| Property | Value |
|---|---:|
| Dataset | Food-101 |
| Number of classes | 101 |
| Images per class | 1,000 |
| Total images | 101,000 |
| Canonical training images per class | 750 |
| Canonical test images per class | 250 |
| InceptionV3 input size | 299 x 299 x 3 |
| EfficientNetV2-S input size in retained run | 384 x 384 x 3 |

Food-101 is appropriate because it is a standard benchmark for food recognition and provides enough class diversity to test transfer learning. It is also limited: the dataset contains curated web images, which may not fully match low-light, occluded, partial, or messy real meal photos captured by users on phones.

Class labels are stored in:

```text
models/ML/final/classes.txt
models/ML/final/labels.txt
```

`classes.txt` uses machine-style labels such as `apple_pie`. `labels.txt` uses display labels such as `Apple Pie`.

---

## Model architecture

The principal deployed model family is InceptionV3.

### Backbone

| Component | Value |
|---|---|
| Backbone | InceptionV3 |
| Pretraining | ImageNet |
| Include original top | No |
| Input tensor | 299 x 299 x 3 |
| Backbone output | 8 x 8 x 2048 |
| Initial backbone state | Frozen |
| Number of Food-101 classes | 101 |

### Custom classification head

The retained model information files describe a regularised head of the following form:

```text
Input image
  -> InceptionV3(include_top=False, ImageNet weights)
  -> Conv2D
  -> BatchNormalization
  -> Activation
  -> SpatialDropout2D(0.3)
  -> Conv2D
  -> BatchNormalization
  -> Activation
  -> SpatialDropout2D(0.3)
  -> MaxPooling2D
  -> GlobalAveragePooling2D
  -> BatchNormalization
  -> Dense(512, relu, L2)
  -> Dropout(0.4)
  -> Dense(256, relu, L2)
  -> Dropout(0.3)
  -> Dense(101, softmax)
```

From the retained `InceptionV3_1.0_model_info.txt`:

| Quantity | Value |
|---|---:|
| Total parameters | 25,636,741 |
| Trainable parameters during head training | 3,830,885 |
| Non-trainable parameters during head training | 21,805,856 |
| Backbone layers | 311 |
| Frozen backbone layers in phase 1 | 311 |

### Prediction function

For class logits `z`, the final layer produces a probability distribution using softmax:

```text
p_i = exp(z_i) / sum_j exp(z_j)
```

For one-hot target vector `y`, categorical cross-entropy is:

```text
L = - sum_i y_i log(p_i)
```

The retained InceptionV3 experiments use categorical cross-entropy with label smoothing `0.1`, which reduces overconfidence by softening hard one-hot targets.

---

## Training methodology

### Preprocessing

For InceptionV3 training, images are resized to `299 x 299` and normalised using the Keras InceptionV3 preprocessing convention. In Keras this maps RGB pixel values into the range expected by the pretrained network, typically `[-1, 1]`.

Training augmentation includes:

| Augmentation | Purpose |
|---|---|
| Random horizontal flip | Reduce viewpoint dependence |
| Random brightness | Simulate lighting variation |
| Random contrast | Improve robustness to image conditions |
| Random saturation | Reduce colour dependence |
| Random hue | Simulate colour shifts |
| Pad and random crop | Simulate zoom and translation |

Validation and test data should not receive stochastic augmentation. They should only receive deterministic resizing and model-specific preprocessing.

### Phase 1: classifier-head training

| Setting | Value |
|---|---|
| Backbone | Frozen |
| Optimizer | Adam |
| Learning rate | 1e-3 initially, reduced in later schedule in retained logs |
| Loss | Categorical cross-entropy |
| Label smoothing | 0.1 |
| Batch size | 32 for `InceptionV3_1.0` |
| Max epochs | 60 for `InceptionV3_1.0` |
| Metrics | Top-1 accuracy, top-5 accuracy, validation loss |

Purpose: train only the new classifier head so gradients do not immediately corrupt pretrained ImageNet features.

### Phase 2: fine-tuning

| Setting | Value |
|---|---|
| Backbone | Partially unfrozen |
| Fine-tuning scope | Later layers only |
| Learning rate | Lower than phase 1 |
| Metrics | Top-1 accuracy, top-5 accuracy, validation loss |
| Checkpoint selection | Best validation metric |

Purpose: adapt high-level features to food-specific visual patterns while preserving low-level visual primitives.

---

## Experimental results

The repository contains several retained experiment folders. Metrics below are computed from saved training logs and should be interpreted as validation metrics from retained artifacts, not final claims about deployment performance on arbitrary phone images.

### InceptionV3 retained results

| Model run | Best validation top-1 | Best validation top-5 | Best validation loss | Notes |
|---|---:|---:|---:|---|
| `InceptionV3` | 77.06% | 93.48% | 1.7490 | Strongest retained Inception run |
| `InceptionV3_1.0` | 76.20% | 93.04% | 1.8146 | Redesigned head, batch size 32 |
| `InceptionV3_0.0` | about 74.75% | about 92.57% | from retained notebook/export | Earlier baseline |

### EfficientNetV2-S retained result

| Model run | Best validation top-1 | Best validation top-5 | Best validation loss | Notes |
|---|---:|---:|---:|---|
| `EfficentNetV2S_1.0` | 87.86% | 97.66% | 1.4164 | Strongest retained benchmark artifact |

### Interpretation

The retained experiments support three conclusions:

1. Transfer learning is effective for Food-101 under this project setup.
2. Fine-tuning improves performance after the classifier head has stabilised.
3. EfficientNetV2-S produces stronger retained validation metrics than the InceptionV3 family, but InceptionV3 remains the current deployed Android TFLite model.

Top-5 accuracy is high relative to top-1 accuracy. That gap matters because food classes are visually ambiguous. A nutrition tracker can exploit top-k predictions by asking users to confirm among plausible classes instead of forcing a single hard prediction.

---

## TensorFlow Lite deployment

The Android classifier loads this asset name:

```text
food_inceptionv3.tflite
```

The current repository contains a model artifact at:

```text
models/food_inceptionv3.tflite
```

The app expects the TFLite model under Android assets:

```text
app/src/main/assets/food_inceptionv3.tflite
```

Because `app/src/main/assets/*` is ignored in `.gitignore`, copy the model locally before building or running inference:

```bash
mkdir -p app/src/main/assets
cp models/food_inceptionv3.tflite app/src/main/assets/food_inceptionv3.tflite
```

The TFLite inference code is implemented in:

```text
app/src/main/java/com/griffith/valuetracker/ml/FoodClassifier.kt
```

Current classifier behaviour:

- Loads `food_inceptionv3.tflite` with `org.tensorflow.lite.Interpreter`.
- Reads image from URI.
- Resizes image to the model input tensor shape.
- Builds a direct `ByteBuffer`.
- Runs inference.
- Returns top-5 raw output indices and scores.

Important ML deployment note: the current Android preprocessing divides RGB values by `255f`, while Keras InceptionV3 training typically uses `preprocess_input`, which maps values to the InceptionV3 expected range. If the exported TFLite model does not include preprocessing internally, Android inference should be changed to match training preprocessing. Otherwise, measured mobile predictions may not reflect offline validation metrics.

---

## Android application

The app is a Kotlin Android application using a local-first architecture.

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Navigation | Navigation Compose |
| Camera | CameraX |
| Image loading | Android Bitmap APIs, Coil for UI image loading |
| ML inference | TensorFlow Lite |
| Dependency injection | Koin |
| Local persistence | Room |
| Preferences | DataStore |
| Build | Gradle Kotlin DSL |

Main Android files:

```text
app/src/main/java/com/griffith/valuetracker/MainActivity.kt
app/src/main/java/com/griffith/valuetracker/navigation/NavGraph.kt
app/src/main/java/com/griffith/valuetracker/ml/FoodClassifier.kt
app/src/main/java/com/griffith/valuetracker/presentation/camera/CameraScreen.kt
app/src/main/java/com/griffith/valuetracker/presentation/camera/ImagePreviewScreen.kt
app/src/main/java/com/griffith/valuetracker/data/DatabaseStorage.kt
```

App flow:

1. User opens camera or selects image.
2. Image URI is passed to preview/classification screen.
3. `FoodClassifier` loads the TFLite model and runs inference.
4. Top outputs are displayed for inspection.
5. Food details, saved foods, meal logging, stats, and settings are handled through Compose screens and local repositories.

---

## Repository structure

```text
.
├── app/                                  Android application
│   ├── build.gradle.kts                  Android build config
│   └── src/main/java/com/griffith/valuetracker/
│       ├── data/                         Room, DataStore, repositories
│       ├── domain/                       Domain models
│       ├── ml/                           TensorFlow Lite classifier
│       ├── navigation/                   Navigation graph
│       ├── presentation/                 Compose screens and ViewModels
│       └── ui/                           Theme and reusable components
├── models/
│   ├── food_inceptionv3.tflite           Local TFLite model artifact
│   └── ML/
│       ├── final/                        Final notebooks, markdown exports, labels
│       └── docs/                         Thesis docs, figures, experiment artifacts
├── build.gradle.kts                      Root Gradle config
├── settings.gradle.kts                   Gradle settings
└── gradlew                               Gradle wrapper
```

Key ML files:

```text
models/ML/final/CNN.ipynb
models/ML/final/CNN.md
models/ML/final/InceptionV3_0.0.ipynb
models/ML/final/EfficientNetV2S_1.1.ipynb
models/ML/final/EfficientNetV2S_2.0.ipynb
models/ML/final/classes.txt
models/ML/final/labels.txt
models/ML/docs/models/InceptionV3_1.0/InceptionV3_1.0_model_info.txt
models/ML/docs/models/InceptionV3_1.0/InceptionV3_1.0_history_finetuned.log
models/ML/docs/models/EfficentNetV2S_1.0/EfficentNetV2S_1.0_history_finetuned.log
```

---

## Setup

### Android requirements

- Android Studio
- JDK 17
- Android SDK matching compile SDK 36
- Gradle wrapper from this repository
- Local TFLite asset copied into `app/src/main/assets/`

### Model asset setup

```bash
mkdir -p app/src/main/assets
cp models/food_inceptionv3.tflite app/src/main/assets/food_inceptionv3.tflite
```

### Build app

```bash
./gradlew assembleDebug
```

---

## Run and test

### Unit tests

```bash
./gradlew test
```

### Android instrumentation tests

Use Android Studio or a connected emulator/device, then run:

```bash
./gradlew connectedAndroidTest
```

Existing tests cover ViewModels, repository behaviour, navigation smoke checks, and Compose screen behaviour.

---

## Known limitations

ML limitations:

- Food-101 validation metrics do not guarantee real-world mobile performance.
- Phone images can differ from Food-101 in lighting, framing, occlusion, and portion composition.
- Similar foods can be visually ambiguous even for humans.
- Current README metrics are taken from saved logs, not from a fresh reproducible run in this checkout.
- EfficientNetV2-S has stronger retained validation performance, but the Android app currently loads an InceptionV3 TFLite file.

Deployment limitations:

- `FoodClassifier.kt` currently returns raw indices and scores rather than mapped class names.
- Label map integration should connect model outputs to `labels.txt` or a bundled Android asset.
- Android preprocessing may not match Keras InceptionV3 preprocessing unless preprocessing was included in the exported TFLite graph.
- No confidence threshold or uncertainty-aware UI is exposed yet.
- No mobile latency, memory, or battery benchmark is currently documented in the README.

Application limitations:

- Nutrition mapping depends on local database coverage and class-name alignment.
- Class-level prediction is not the same as portion estimation.
- A predicted class alone cannot accurately determine calories without serving size and ingredients.

---

## Future work

Highest-value ML improvements:

1. Add exact Android label-map loading and display human-readable top-k predictions.
2. Make Android preprocessing identical to training preprocessing.
3. Export and test the stronger EfficientNetV2-S model as TFLite.
4. Add post-training quantization and compare FP32, FP16, and INT8 models.
5. Report mobile inference latency, memory use, APK size, and thermal behaviour.
6. Add confidence calibration and top-k confirmation UI.
7. Produce per-class precision, recall, F1, and confusion analysis from a reproducible evaluation script.
8. Add a small real-phone image test set to measure domain shift from Food-101 to actual user photos.

Highest-value product improvements:

1. Map predictions directly to nutrition records.
2. Add user confirmation before meal logging.
3. Store top-k predictions for later error analysis.
4. Support serving-size estimation or user-adjusted portion size.
5. Add model/version metadata to logged predictions for experiment traceability.

---

## Project thesis framing

Working thesis framing:

> Food image classification for mobile nutrition tracking can be approached as a transfer-learning problem, but its practical value depends on deployment details: preprocessing consistency, label mapping, confidence handling, local inference cost, and integration with a usable meal-logging workflow.

This repository therefore evaluates both sides of the problem: the CNN model as a machine learning artifact and the Kotlin Android app as a deployment environment for that model.
