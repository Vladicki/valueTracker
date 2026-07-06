FROM tensorflow/tensorflow:2.18.0-gpu-jupyter


RUN apt-get update && \
    apt-get install -y libcudnn9-cuda-12 libcudnn9-dev-cuda-12 && \
    ln -sf /usr/lib/x86_64-linux-gnu/libcudnn.so.9 /usr/lib/x86_64-linux-gnu/libcudnn.so && \
    ldconfig && \
    rm -rf /var/lib/apt/lists/*



# Create folders
RUN mkdir -p /tf/ML /tf/data

WORKDIR /tf
