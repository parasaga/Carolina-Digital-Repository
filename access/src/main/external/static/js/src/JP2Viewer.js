define([ 'jquery', 'jquery-ui', 'openLayers'], function($, ui) {
	$.widget("cdr.jp2Viewer", {
		options : {
			context : "",
			show : false
		},
		
		_create : function() {
			this.initialized = false;
			this.pid = this.element.attr("data-pid");
			if (show) {
				this.show();
			}
		},
		
		show : function() {
			if (!this.initialized)
				this._initDjatokaLayers();
		},
		
		_initDjatokaLayers : function() {
			var metadataUrl = 'jp2Metadata/' + this.options.pid.replace(':', '/') + '/IMAGE_JP2000',
				self = this;
			
			OpenLayers.Layer.OpenURL.djatokaURL = this.options.context + '/jp2Region/' + this.pid.replace(':', '/') + '/IMAGE_JP2000';
			OpenLayers.Layer.OpenURL.viewerWidth = this.element.width(); // Use viewer width
			OpenLayers.Layer.OpenURL.viewerHeight = this.element.height(); // Use viewer height
			
			
			// define the layer (content, filetypes, etc)
			var OUlayer = new OpenLayers.Layer.OpenURL("OpenURL", "", {
					layername: 'basic',
					format:'image/jpeg',
					rft_id: "ignore",
					height: 200,
					width: 200,
					metadataUrl: metadataUrl
				});
			var metadata = OUlayer.getImageMetadata();
			// bail out if no metadata could be retrieved
			if ($.isEmptyObject(metadata)) {
				this.element.hide();
				$('#viewer_bar').hide();
				return;
			};
			var resolutions = OUlayer.getResolutions();
			var maxExtent = new OpenLayers.Bounds(0, 0, metadata.width, metadata.height);
			var tileSize = OUlayer.getTileSize();
			var options = {
				resolutions: resolutions,
				maxExtent: maxExtent,
				tileSize: tileSize,
				controls: [ new OpenLayers.Control.Navigation(),
							new OpenLayers.Control.UNCPanZoomBar(),
							new OpenLayers.Control.ArgParser(),
							new OpenLayers.Control.Attribution()
				]
			};
			
			OUlayer.events.register("loadend", OUlayer, function() {
				this.element.removeClass("not_loaded");
		    });
			
			OpenLayers.Util.onImageLoadError = function(){
				this.element.removeClass("not_loaded").height("30px")
					.html("<div class='error'>Sorry, an error occurred while loading the image.</div>");
			};

			// Create the image_viewer
			var map = new OpenLayers.Map(viewer_id, options);
			map.addLayer(OUlayer);
			var lon = metadata.width / 2;
			var lat = metadata.height / 2;
			map.setCenter(new OpenLayers.LonLat(lon, lat), 0);
			
			var fullScreen = new OpenLayers.Control.Button({title: "Toggle full screen mode",
			    displayClass: "ol_fullscreen", trigger: function(){
			    	if (self.element.hasClass("full_screen")){
			    		self.element.removeClass("full_screen");
			    		$(document).unbind("keyup");
			    	} else {
			    		self.element.addClass("full_screen");
			    		$(document).keyup(function(e) {
			    			if (e.keyCode == 27) {
			    				self.element.removeClass("full_screen");
			    				map.updateSize();
			    				$(document).unbind("keyup");
			    			}
			    		});
			    	}
			    	map.updateSize();
			    	window.scrollTo(0, 0);
			    }
			});
			var panel = new OpenLayers.Control.Panel({defaultControl: fullScreen, 
				displayClass: "ol_fullscreen_panel"});
			
			panel.addControls([fullScreen]);
			map.addControl(panel);
		}
	});
});