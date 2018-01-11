#!/system/bin/sh

echo "load" > /sys/class/leds/d601:B/device/engine1_mode
echo "load" > /sys/class/leds/d601:B/device/engine2_mode
echo "load" > /sys/class/leds/d601:B/device/engine3_mode
echo "load" > /sys/class/leds/d604:B/device/engine1_mode
echo "load" > /sys/class/leds/d604:B/device/engine2_mode
echo "load" > /sys/class/leds/d604:B/device/engine3_mode
echo "load" > /sys/class/leds/d610:B/device/engine1_mode
echo "load" > /sys/class/leds/d610:B/device/engine2_mode
echo "load" > /sys/class/leds/d610:B/device/engine3_mode
echo "load" > /sys/class/leds/d607:B/device/engine1_mode
echo "load" > /sys/class/leds/d607:B/device/engine2_mode
echo "load" > /sys/class/leds/d607:B/device/engine3_mode
echo "00039F8040004600E00C04C8C000" > /sys/class/leds/d601:B/device/engine1_load
echo "000C9F904000E080500004C8C000" > /sys/class/leds/d601:B/device/engine2_load
echo "00309FA04000E0805E0004C8C000" > /sys/class/leds/d601:B/device/engine3_load
echo "00039F804000E00C6E0004C8C000" > /sys/class/leds/d604:B/device/engine1_load
echo "000C9F904000E0807E0004C8C000" > /sys/class/leds/d604:B/device/engine2_load
echo "00309FA04000E0807E00500004C8C000" > /sys/class/leds/d604:B/device/engine3_load
echo "00039F804000E00C7E005E0004C8C000" > /sys/class/leds/d610:B/device/engine1_load
echo "000C9F904000E0807E006E0004C8C000" > /sys/class/leds/d610:B/device/engine2_load
echo "00309FA04000E0807E007E0004C8C000" > /sys/class/leds/d610:B/device/engine3_load
echo "00039F804000E00C7E007E00500004C8C000" > /sys/class/leds/d607:B/device/engine1_load
echo "000C9F904000E0807E007E005E0004C8C000" > /sys/class/leds/d607:B/device/engine2_load
echo "00309FA04000E0807E007E006E0004C8C000" > /sys/class/leds/d607:B/device/engine3_load

echo "run" | tee /sys/class/leds/d607:B/device/engine1_mode /sys/class/leds/d610:B/device/engine1_mode /sys/class/leds/d604:B/device/engine1_mode /sys/class/leds/d601:B/device/engine1_mode
