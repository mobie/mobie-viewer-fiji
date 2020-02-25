# MMB Fiji

Fiji plugin for the exploration of multi-modal big image data. Currently, it is used to implemnt the PlatyBrowser, a tool to explore a cellular atlas for the biological model system _Platynereis dumerilii_.


## Installation

1. Please [install Fiji](https://fiji.sc) on your computer.
1. Open Fiji and install the EMBL-CBA update site ([how to install an update site](https://imagej.net/Following_an_update_site#Introduction)).<br><img width="460" alt="image" src="https://github.com/platybrowser/mmb-fiji/blob/master/images/74152530-c9603500-4c0e-11ea-81d8-d518b9ed1ef3.png"> <br> Select the EMBL-CBA update site and then click [ Close ]
1. Restart Fiji

## Starting up

1. In the Fiji search bar, type: "Open Platy"<br> <img width="460" alt="image" src="https://github.com/platybrowser/mmb-fiji/blob/master/images/74152904-98cccb00-4c0f-11ea-9819-6c772174a2c0.png"> <br> ...and click [ Run ]
1. Select the version of the data: <br><img width="200" alt="image" src="https://github.com/platybrowser/mmb-fiji/blob/master/images/version.jpg">
1. The PlatyBrowser is ready to be used:<br><img width="800" alt="image" src="https://github.com/platybrowser/mmb-fiji/blob/master/images/main-ui-bdv.jpg">

## Operation

### Buttons in main panel
- [ help ] Please select an item from the drop-down and click the [ help ] button. A corresponding help page will appear.
- [ add ] Please select an image source from the drop-downs and click the corresponding [ add ] button. The image source will be added to the current view. If the image source is a segmentation, also a corresponding objects table will appear. 
- [ view ] Please select a bookmark item from the drop-down and click the [ view ] button. The corresponding bookmarked view will appear. A “bookmark” can consist of multiple items, such as different image layers, as well as object selections, and coloring modes.
- [ move ] Please enter a x, y, z position (in micrometer units) as comma separated values, as an example you may try: `120.5,115.3,201.5`. Clicking [ move ] will move the current viewer such that the entered position is in the centre of the viewer. You may also enter 12 comma separated values; in this case the view will be positioned according to the affine transformation that is specified by these numbers; as an example, you may try: ``.
- [ level ] Clicking this button will level the current view such that the dorso-ventral axis of the animal is perpendicular to the viewing plane, and the anterior part of the animal facing upwards. This view is suited to inspect the the bilateral symmetry of the specimen.

### Image sources buttons

Adding an image source to BigDataViewer will also make it appear in the image **sources panel**, such as shown in below example.

<img width="220" alt="image" src="https://github.com/platybrowser/mmb-fiji/blob/master/images/74252332-d2740380-4ced-11ea-9a4e-d25cb1e83859.png">

To control the image sources appearance, there are several buttons...

- [ C ] Change the color
- [ B ] Change the brightness settings
- [ X ] Remove from viewer and sources panel

...and checkboxes:

- [ X ] V Uncheck to remove from volume viewer (but keep in sources panel)  
- [ X ] S Uncheck to remove from BigDataViewer (but keep in sources panel)  

### Keyboard shortcuts in BigDataViewer window

There are many useful keyboard shortcuts. Please consult the [ help ]  button in the main panel for valuable information.

### Exploring segmentations

Viewing a _segmentation_ will show the segmented objects as a coloured overlay in the viewer and, if available, also show a corresponding table where each row corresponds to one segmented object in the image. 

#### Interacting with image segments in BigDataViewer

Image segments can be interacted with in the BigDataViewer window, please see [here](https://htmlpreview.github.io/?https://github.com/tischi/table-utils/blob/master/src/main/resources/SegmentationImageActionsHelp.html).

#### Interacting with image segments in the table

The image segments table is interactive.

- Clicking on a row will select an object; this will also center and highlight the object in BigDataViewer.
- Clicking on a column header will sort the table by the values in that column.

Moreover, the table has its own menu. Important menu entries include:

- [ Color > Color by Column... ] Use this to color the image segments by the feature values of any column.
- [ Table > Append Table...] By default only minimal information about image segments is shown. Use this menu entry to append more measurements, as stored in other tables. 
- [ Annotate > Start new annotation...] Use this to perform a manual annotation of image segments. Performing this action will add a new column to the table, containing your annotations.
- [ Table > Save Table As...] Use this to store the table to disk. This is useful to, e.g., save manual image segment annotations.


### Advanced options

By default the PlatyBrowser plugin fetches the data from github and a publicly accessible object store. However it can also be configured to fetch the data from other locations, such as a local file-server. To access this functionality please type in Fiji’s search bar: "Open PlatyBrowser Advanced"

