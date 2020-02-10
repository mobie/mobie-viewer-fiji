# PlatyBrowser Fiji

Plugin for visualisation of multi-modal big image data of the biological model system Platynereis dumerilii.


Installation: 
If you don’t yet have a Fiji installed on your computer, please install from this web site: https://fiji.sc
Open Fiji and install the EMBL-CBA update site, following this instructions: https://imagej.net/Following_an_update_site#Introduction
Select the EMBL-CBA update site and then click [ Close ]

Please restart Fiji
Starting up: 
In the Fiji search bar, type: PlatyBrowser
Put an image
Select the first item in the list and click [ Run ]
The PlatyBrowser is ready to be used
Put an image
Operation: 
Buttons in main panel:
[ help ]: Please select an item from the drop-down and click the [ help ] button. A corresponding help page will appear.
[ add ]: Please select an image source from the drop-downs and click the corresponding [ add ] button. The image source will be added to the current view. If the image source is a segmentation, also a corresponding objects table will appear. 
[ view ]: Please select a bookmark item from the drop-down and click the [ view ] button. The corresponding bookmarked view will appear. A “bookmark” can consist of multiple items, such as different image layers, as well as object selections, and coloring modes.
[ move ]: Please enter a x, y, z position (in micrometer units) as comma separated values, as an example you may try: x,y,z . Clicking [ move ] will move the current viewer such that the entered position is in the centre of the viewer. You may also enter 12 comma separated values; in this case the view will be positioned according to the affine-transformation that is specified by these numbers. As an example, try: ,,,,, 
[ level ]: Clicking this button will level the current view such that the dorso-ventral axis of the animal is perpendicular to the viewing plane, and the anterior part of the animal facing upwards. This view is suited to inspect the the bilateral symmetry of the specimen.

Checkboxes in main panel:
[ X ] show volumes in 3D: Checking this will show the ProSPr image data not only in the BigDataViewer window but also show a 3D rendering in ImageJ’s 3D Image Viewer  (Schmid et al., 2010)
[ X ] show objects in 3D: Checking this will show a 3D rendering of selected objects  in ImageJ’s 3D Image Viewer  (Schmid et al., 2010)
Keyboard shortcuts:
There are many useful keyboard shortcuts. Please consult the [ help ]  button in the main panel for valuable information.
Objects table:
Viewing a segmentation will in most cases automatically load and render a corresponding table where each row corresponds to one segmented object in the image. The table has a menu bar with several useful commands, among them is a Help menu item that can be consulted for more information.
Advanced options:
By default the PlatyBrowser plugin fetches the data from github and a publicly accessible object store. However it can also be configured to fetch the data from other locations, such as a local file-server. To access this functionality please type in Fiji’s search bar: PlatyBrowserAdvanced 

