#!/bin/bash

# You will need an installation of OpenMPI for this installation to succeed.

if [ -z $1 ]; then
    echo "Usage: ./install.sh [Optional: [--boost] [--pi3]]"
    echo "BOOST_ROOT is the root directory of your Boost C++ library"
    exit 1
fi

cd $HOME
mkdir lib
cd lib

# MPICH install on Raspberry PI 3
if [ $2="--pi3"]; then
    # We use MPICH because OpenMPI does not support ARMvl CPU architecture
    wget http://www.mpich.org/static/downloads/3.2/mpich-3.2.tar.gz
    tar -zxvf mpich-3.2.tar.gz
    rm mpich-3.2.tar.gz
    cd mpich-3.2
    sudo ./configure --prefix=/usr/local --disable-fortran
    sudo make && make install
    cd ..
fi


# Boost installation
if [ $1="--boost" ]; then
    wget -c 'http://sourceforge.net/projects/boost/files/boost/1.61.0/boost_1_61_0.tar.bz2/download'
    tar xf download
    rm download
    cd boost_1_61_0
    echo "using mpi ;" > user-config.jam
    sudo ./bootstrap.sh --prefix=/usr/local
    sudo ./bjam --user-config=user-config.jam --with-mpi install
    cd ..
fi


