FROM ubuntu:20.04

MAINTAINER Hoang Lam Nguyen <nguyehoa@informatik.hu-berlin.de>

ARG DEBIAN_FRONTEND=noninteractive

# Main setup
RUN apt-get -y update && \
    apt-get -y install build-essential python3-pip maven vim && \
    ln -s /usr/bin/python3 /usr/bin/python

# Install python packages from requirements.txt
COPY requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

WORKDIR /workspace