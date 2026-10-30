#!/usr/bin/env python3
"""One consolidated CPU/GPU-capable MobileNetV3Small training and export run.

Usage:
  python3 ml/train_sky_cnn.py /path/to/WEBCAM_v1.zip

The seven output labels map only the matching WEBCAM directory labels. Ac and Ci are
intentionally excluded; they are not relabelled as a different target class.
"""
from __future__ import annotations
import argparse, collections, hashlib, json, os, pathlib, re, time, zipfile
import numpy as np
import tensorflow as tf
from sklearn.model_selection import GroupShuffleSplit
from sklearn.metrics import accuracy_score, confusion_matrix, precision_recall_fscore_support, f1_score

SEED = 41
IMAGE_SIZE = 160
BATCH_SIZE = 24
EPOCHS = 8
CLASS_DIRS = {
    "clear": "clear",
    "cu": "fair_weather_cumulus",
    "st": "overcast_or_stratus",
    "tcu": "towering_cumulus",
    "cb": "cumulonimbus_or_storm_like",
    "obsc": "fog_or_obscured_sky",
    "precip": "rain_or_precipitation",
}
LABELS = list(CLASS_DIRS.values())


def camera_day_group(filename: str) -> str:
    """Keep captures from the same identified camera and date in one partition."""
    name = pathlib.PurePosixPath(filename).name
    date = None
    # Axis-site_YYYYMMDD_HHMM; east_YYYY-MM-DD_HH-MM and similar; timestamped IDs.
    m = re.search(r"(20\d{6})", name)
    if m:
        date = m.group(1)
    if not date:
        m = re.search(r"(\d{2})-(\d{2})-(\d{2})", name)
        if m:
            date = "20" + m.group(3) + m.group(1) + m.group(2)
    date = date or "unknown-date"
    if name.startswith("Axis-"):
        site = re.split(r"_20\d{6}", name, maxsplit=1)[0]
    elif re.match(r"^20\d{6}", name):
        m = re.match(r"^20\d{6}\.([^.]+)\.([^.]+)", name)
        site = "numeric-camera-" + ("-".join(m.groups()) if m else "unknown")
    else:
        # Direction-keyed names have no further exposed station ID; group by view and day.
        site = name.split("_")[0].lower()
    return site + "__" + date


def extract_and_manifest(archive: pathlib.Path, out: pathlib.Path):
    rows = []
    out.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        for info in z.infolist():
            parts = info.filename.split("/")
            if len(parts) < 3 or parts[1].lower() not in CLASS_DIRS:
                continue
            if not info.filename.lower().endswith((".jpg", ".jpeg")):
                continue
            cls = CLASS_DIRS[parts[1].lower()]
            target = out / cls / pathlib.Path(parts[-1]).name
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists():
                with z.open(info) as src, open(target, "wb") as dst:
                    dst.write(src.read())
            rows.append((str(target), LABELS.index(cls), camera_day_group(info.filename), info.filename))
    return rows


def dataset(paths, labels, training=False):
    ds = tf.data.Dataset.from_tensor_slices((paths, labels))
    if training:
        ds = ds.shuffle(min(len(paths), 12000), seed=SEED, reshuffle_each_iteration=True)
    def load(path, label):
        data = tf.io.read_file(path)
        image = tf.io.decode_jpeg(data, channels=3, try_recover_truncated=True)
        image = tf.image.resize_with_pad(image, IMAGE_SIZE, IMAGE_SIZE, antialias=True)
        image = tf.cast(image, tf.float32)
        return image, label
    ds = ds.map(load, num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=pathlib.Path)
    parser.add_argument("--output", type=pathlib.Path, default=pathlib.Path("."))
    parser.add_argument("--work", type=pathlib.Path, default=pathlib.Path("/tmp/solar-webcam-extracted"))
    args = parser.parse_args()
    tf.keras.utils.set_random_seed(SEED)
    started = time.time()
    digest = hashlib.sha256()
    with args.archive.open("rb") as stream:
        for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            digest.update(block)
    archive_sha = digest.hexdigest()
    rows = extract_and_manifest(args.archive, args.work)
    if not rows:
        raise RuntimeError("No images found in selected WEBCAM label folders")
    by_class = collections.Counter(LABELS[label] for _, label, _, _ in rows)
    groups = np.array([r[2] for r in rows])
    y = np.array([r[1] for r in rows])
    paths = np.array([r[0] for r in rows])
    # Sequence/site-day groups do not cross train/validation/test partitions.
    gss_test = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=SEED)
    trainval_idx, test_idx = next(gss_test.split(paths, y, groups))
    gss_val = GroupShuffleSplit(n_splits=1, test_size=0.125, random_state=SEED + 1)
    train_rel, val_rel = next(gss_val.split(paths[trainval_idx], y[trainval_idx], groups[trainval_idx]))
    train_idx, val_idx = trainval_idx[train_rel], trainval_idx[val_rel]
    split_groups = [set(groups[i] for i in idx) for idx in (train_idx, val_idx, test_idx)]
    if split_groups[0] & split_groups[1] or split_groups[0] & split_groups[2] or split_groups[1] & split_groups[2]:
        raise AssertionError("Capture group leakage across partitions")
    for name, idx in zip(("train", "validation", "test"), (train_idx, val_idx, test_idx)):
        counts = collections.Counter(LABELS[y[i]] for i in idx)
        print(name, len(idx), "groups", len(set(groups[idx])), dict(counts), flush=True)
        if any(counts.get(label, 0) == 0 for label in LABELS):
            raise RuntimeError(f"{name} split has no examples for at least one requested class; do not report a misleading metric")

    train_ds = dataset(paths[train_idx], y[train_idx], training=True)
    val_ds = dataset(paths[val_idx], y[val_idx])
    test_ds = dataset(paths[test_idx], y[test_idx])
    augmenter = tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomContrast(0.12),
    ], name="training_augmentation")
    base = tf.keras.applications.MobileNetV3Small(
        input_shape=(IMAGE_SIZE, IMAGE_SIZE, 3), include_top=False, weights="imagenet",
        include_preprocessing=True
    )
    # One fine-tuning run; keep batch-normalization statistics fixed for the small dataset.
    base.trainable = True
    for layer in base.layers[:-35]:
        layer.trainable = False
    for layer in base.layers:
        if isinstance(layer, tf.keras.layers.BatchNormalization):
            layer.trainable = False
    inputs = tf.keras.Input((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=tf.float32, name="rgb_0_255")
    x = augmenter(inputs)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    outputs = tf.keras.layers.Dense(len(LABELS), activation="softmax", name="sky_class_probabilities")(x)
    model = tf.keras.Model(inputs, outputs, name="astra_mobilenet_v3_small")
    model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=2e-4),
                  loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    counts = np.bincount(y[train_idx], minlength=len(LABELS))
    class_weights = {i: float(len(train_idx) / (len(LABELS) * max(counts[i], 1))) for i in range(len(LABELS))}
    callbacks = [
        tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=2, restore_best_weights=True),
        tf.keras.callbacks.TerminateOnNaN(),
    ]
    # Exactly one model.fit training run; no competing architecture/second experiment.
    history = model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS,
                        class_weight=class_weights, callbacks=callbacks, verbose=2)

    y_test, prob_test = [], []
    for xb, yb in test_ds:
        y_test.extend(yb.numpy().tolist())
        prob_test.extend(model.predict(xb, verbose=0).tolist())
    y_test = np.asarray(y_test, dtype=int)
    prob_test = np.asarray(prob_test, dtype=np.float32)
    y_val, prob_val = [], []
    for xb, yb in val_ds:
        y_val.extend(yb.numpy().tolist())
        prob_val.extend(model.predict(xb, verbose=0).tolist())
    y_val = np.asarray(y_val, dtype=int)
    prob_val = np.asarray(prob_val, dtype=np.float32)
    # Select the lowest validation confidence cutoff meeting the documented selective-accuracy
    # target with useful coverage. This is threshold selection, not probability calibration.
    threshold_results = []
    for threshold in np.arange(0.30, 0.951, 0.01):
        keep = prob_val.max(axis=1) >= threshold
        coverage = float(keep.mean())
        selective_accuracy = float(accuracy_score(y_val[keep], prob_val[keep].argmax(axis=1))) if keep.any() else 0.0
        threshold_results.append((float(threshold), coverage, selective_accuracy))
    eligible = [x for x in threshold_results if x[1] >= 0.20 and x[2] >= 0.80]
    confidence_threshold = eligible[0][0] if eligible else 0.70
    threshold_validation = next(({"threshold": t, "coverage": c, "selective_accuracy": a}
                                 for t, c, a in threshold_results if abs(t-confidence_threshold) < 1e-6), None)
    pred = prob_test.argmax(axis=1)
    p, r, f, _ = precision_recall_fscore_support(y_test, pred, labels=range(len(LABELS)), zero_division=0)

    args.output.mkdir(parents=True, exist_ok=True)
    assets = args.output / "app/src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    lite = converter.convert()
    model_path = assets / "sky_conditions.tflite"
    model_path.write_bytes(lite)
    (assets / "sky_conditions_labels.json").write_text(json.dumps({"input_size": IMAGE_SIZE, "labels": LABELS,
      "minimum_confidence": confidence_threshold, "dataset": "WEBCAM v1.0"}, indent=2) + "\n")

    # Measure interpreter latency on this sandbox CPU; not a claim about a target phone.
    interpreter = tf.lite.Interpreter(model_path=str(model_path), num_threads=2)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]; out = interpreter.get_output_details()[0]
    sample = np.zeros(inp["shape"], dtype=inp["dtype"])
    for _ in range(5): interpreter.set_tensor(inp["index"], sample); interpreter.invoke()
    latencies = []
    for _ in range(30):
        t0 = time.perf_counter(); interpreter.set_tensor(inp["index"], sample); interpreter.invoke()
        latencies.append((time.perf_counter() - t0) * 1000)
    metrics = {
        "model": "MobileNetV3Small fine-tuned from ImageNet weights",
        "dataset": "WEBCAM v1.0", "dataset_release_url": "https://github.com/MarcusCoteFIT/webcam-ground-based-cloud-image-dataset/releases/tag/v1.0",
        "dataset_archive_sha256": archive_sha, "dataset_images_used": len(rows), "all_selected_class_counts": dict(by_class),
        "excluded_source_labels": {"ac": "altocumulus has no dedicated target class", "ci": "cirrus has no dedicated target class"},
        "class_order": LABELS, "grouping": "capture source/camera token plus capture day; groups disjoint across partitions",
        "split_counts": {name: {"images": len(idx), "groups": len(set(groups[idx])), "classes": dict(collections.Counter(LABELS[y[i]] for i in idx))}
                         for name, idx in zip(("train", "validation", "test"), (train_idx, val_idx, test_idx))},
        "epochs_completed": len(history.history["loss"]), "heldout_test": {
            "accuracy": float(accuracy_score(y_test, pred)), "macro_f1": float(f1_score(y_test, pred, average="macro")),
            "per_class_precision": {LABELS[i]: float(p[i]) for i in range(len(LABELS))},
            "per_class_recall": {LABELS[i]: float(r[i]) for i in range(len(LABELS))},
            "per_class_f1": {LABELS[i]: float(f[i]) for i in range(len(LABELS))},
            "confusion_matrix_rows_true_cols_pred": confusion_matrix(y_test, pred, labels=range(len(LABELS))).tolist(),
        },
        "confidence_threshold": confidence_threshold,
        "threshold_policy": "Lowest validation cutoff targeting >=80% selective accuracy at >=20% coverage; 0.70 fallback if no cutoff met. Softmax score is not a calibrated storm probability.",
        "validation_threshold_result": threshold_validation,
        "model_file": str(model_path), "model_bytes": len(lite),
        "sandbox_cpu_latency_ms_30_runs_after_5_warmups": {"median": float(np.median(latencies)), "p95": float(np.percentile(latencies, 95))},
        "device_latency_note": "Measured on this sandbox CPU only; target Android phone not benchmarked.",
        "dataset_license_note": "WEBCAM release/paper state public availability for research purposes; no dataset-specific license was present in inspected source. This derived model is delivered only for non-commercial evaluation; confirm permissions before commercial redistribution.",
        "training_wall_seconds": round(time.time() - started, 1), "seed": SEED, "image_size": IMAGE_SIZE,
        "training_configuration": {"optimizer": "Adam", "learning_rate": 0.0002, "batch_size": BATCH_SIZE,
          "maximum_epochs": EPOCHS, "early_stopping": "val_loss patience=2 restore_best_weights",
          "fine_tuned_layers": "MobileNetV3Small last 35 non-BatchNorm layers; earlier layers frozen",
          "augmentation": ["horizontal flip", "contrast 0.12"], "class_imbalance": "inverse-frequency class weights",
          "partition_seed": SEED, "partitioning": "GroupShuffleSplit by camera/site token + capture date; test 20%, validation 10% approx."},
    }
    (args.output / "ml/sky_model_evaluation.json").write_text(json.dumps(metrics, indent=2) + "\n")
    (args.output / "ml/sky_model_training_config.json").write_text(json.dumps(metrics["training_configuration"], indent=2) + "\n")
    print(json.dumps(metrics, indent=2), flush=True)

if __name__ == "__main__":
    main()
