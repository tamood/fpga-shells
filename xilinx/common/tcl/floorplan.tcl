create_pblock coreblock
add_cells_to_pblock coreblock [get_cells coreplex]  -clear_locs -quiet
resize_pblock -add {CLOCKREGION_X0Y0:CLOCKREGION_X0Y3 CLOCKREGION_X1Y0:CLOCKREGION_X1Y1 CLOCKREGION_X1Y3:CLOCKREGION_X1Y3} coreblock

create_pblock p1block
add_cells_to_pblock p1block [get_cells coreplex/tile_1]  -clear_locs -quiet
resize_pblock -add {CLOCKREGION_X0Y0:CLOCKREGION_X1Y0 SLICE_X42Y50:SLICE_X51Y99} p1block

create_pblock p0block
add_cells_to_pblock p0block [get_cells coreplex/tile]  -clear_locs -quiet
resize_pblock -add {CLOCKREGION_X0Y3:CLOCKREGION_X1Y3 SLICE_X42Y100:SLICE_X51Y149} p0block

create_pblock memblock
add_cells_to_pblock memblock [get_cells coreplex/ddr*]  -clear_locs -quiet
resize_pblock -add {CLOCKREGION_X1Y1:CLOCKREGION_X1Y1} memblock

create_pblock ioblock
add_cells_to_pblock ioblock [get_cells {coreplex/gpio* coreplex/uart* coreplex/spi* coreplex/maskROM}]  -clear_locs -quiet
resize_pblock -add {SLICE_X0Y50:SLICE_X7Y149 RAMB18_X0Y20:RAMB18_X0Y59 RAMB36_X0Y10:RAMB36_X0Y29} ioblock

set_property PARENT coreblock [get_pblocks {p0block p1block memblock ioblock}] 