#!/bin/sh
magick convert DMM.png                           -write mpr:img     \
\( mpr:img -resize 16x16                         +write x16.png  \) \
\( mpr:img -resize 32x32                         +write x32.png  \) \
\( mpr:img -resize 64x64                         +write x64.png  \) \
\( mpr:img -resize 128x128                       +write x128.png \) \
\( mpr:img -resize 256x256                       +write x256.png \) \
\( mpr:img -define icon:auto-resize=16,32,64,128 +write DMM.ico  \) \
null: