module VGA(
    input clk,
    input rst,
    
    output hsync,
    output vsync,
    
    output reg [11:0] rgb
);

    // VGA timings https://timetoexplore.net/blog/video-timings-vga-720p-1080p
    localparam HS_STA = 16;              // horizontal sync start
    localparam HS_END = 16 + 96;         // horizontal sync end
    localparam HA_STA = 16 + 96 + 48;    // horizontal active pixel start
    localparam VS_STA = 480 + 10;        // vertical sync start
    localparam VS_END = 480 + 10 + 2;    // vertical sync end
    localparam VA_END = 480;             // vertical active pixel end
    localparam LINE   = 800;             // complete line (pixels)
    localparam SCREEN = 525;             // complete screen (lines)
    
    wire line_end;
    wire screen_end;
    wire rgb_valid;

    reg [9:0] h_count;
    reg [9:0] v_count;

    // generate sync signals (active low for 640x480)
    assign hsync = ~((h_count >= HS_STA) & (h_count < HS_END));
    assign vsync = ~((v_count >= VS_STA) & (v_count < VS_END));
    
    assign screen_end = (v_count == (SCREEN-1));
    assign line_end = (h_count == (LINE-1));
    assign rgb_valid = (h_count >= HA_STA) & (v_count < VA_END);

    always @ (posedge clk)
      if (rst | line_end)
	h_count <= 0;
      else
	h_count <= h_count + 1;
	
	
    always @ (posedge clk)
      if (rst)
	v_count <= 0;
      else if (line_end)
	if (screen_end)
	  v_count <= 0;
	else
	  v_count <= v_count + 1;
	  
    always @ (posedge clk)
      if (rst)
	rgb <= 0;
      else if (rgb_valid)
	rgb <=	rgb + 1;

endmodule 
