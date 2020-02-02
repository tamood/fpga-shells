# See LICENSE for license details.

open_hw_manager

connect_hw_server -allow_non_jtag

open_hw_target

set device [get_hw_devices]

current_hw_device $device

set bitfile [lindex $argv 0]

set_property PROGRAM.FILE $bitfile $device

program_hw_devices 
