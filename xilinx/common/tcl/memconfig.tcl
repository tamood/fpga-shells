# Simple Vivado script to program a SPI flash

open_hw_manager

connect_hw_server -allow_non_jtag

open_hw_target

set memPart [lindex [get_cfgmem_parts {s25fl128sxxxxxx0-spi-x1_x2_x4}] 0]

set infile [lindex $argv 0]

set memDevice [create_hw_cfgmem -hw_device [lindex [get_hw_devices] 0] -mem_dev $memPart]

set_property PROGRAM.ADDRESS_RANGE {use_file} $memDevice
set_property PROGRAM.FILES $infile $memDevice
set_property PROGRAM.UNUSED_PIN_TERMINATION {pull-none} $memDevice

program_hw_devices [lindex [get_hw_devices] 0]

set_property PROGRAM.BLANK_CHECK 0 $memDevice
set_property PROGRAM.ERASE 1 $memDevice
set_property PROGRAM.CFG_PROGRAM 1 $memDevice
set_property PROGRAM.VERIFY 0 $memDevice
 
program_hw_cfgmem -verbose $memDevice
