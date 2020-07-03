# MoBIE Fiji Viewer

The MoBIE Fiji viewer is a Fiji plugin for the exploration of big multi-modal image and associated tabular data. 

## History

The MoBIE was initially develped to explore a cellular atlas for the biological model system _Platynereis dumerilii_, see: [Whole-body integration of gene expression and single-cell morphology](https://www.biorxiv.org/content/10.1101/2020.02.26.961037v1). However, the framework turned out to be so useful and generalisable that it is now used for other projects as well, e.g. see all the repositories ending on "-datasets" [here](https://github.com/mobie).

## Installation

1. Please [install Fiji](https://fiji.sc) on your computer.
2. Open Fiji and install the MoBIE update site ([how to install an update site](https://imagej.net/Following_an_update_site#Introduction)).
3. Restart Fiji

## Starting up

1. In the Fiji search bar, type: "mobie"<br> <img width="460" alt="image" src="https://user-images.githubusercontent.com/2157566/86445323-79dfea00-bd12-11ea-8884-5e50a08660d0.png"> <br> ...and click [ Run ]
2. Enter a github repository representing your datasets <br><img width="300" alt="image" src="https://user-images.githubusercontent.com/2157566/86445504-cdeace80-bd12-11ea-996a-4a6d5d58ccc7.png">
3. The MoBIE viewer is ready to be used:<br><img width="800" alt="image" src="https://user-images.githubusercontent.com/2157566/86445771-42be0880-bd13-11ea-9627-cd1ee7b62a99.png">

## Operation

### Buttons in main panel
- [ info ] Please select an item from the drop-down and click the button. A corresponding info page will appear.
- [ switch ] Please select an item from the drop-down and click the button. The dataset will be switched. Note that a MoBIE datasets repository can contain multiple datasets. The definition of one dataset is that all images in this dataset can be meaningfully displayed in the same physical coordinate system.
- [ add ] Please select an image source from the drop-downs and click the button. The image source will be added to the current view. If the image source is a segmentation, also a corresponding objects table will appear. 
- [ view ] Please select a bookmark item from the drop-down and click the [ view ] button. The corresponding bookmarked view will appear. A “bookmark” can consist of multiple items, such as different image layers, as well as object selections, and coloring modes.
- [ move ] Please enter a x, y, z position (in micrometer units) as comma separated values, as an example you may try: `120.5,115.3,201.5`. Clicking [ move ] will move the current viewer such that the entered position is in the centre of the viewer. You may also enter 12 comma separated values; in this case the view will be positioned according to the affine transformation that is specified by these numbers; as an example, you may try: ``.
- [ level ] Clicking this button will level the current view such that the dorso-ventral axis of the animal is perpendicular to the viewing plane, and the anterior part of the animal facing upwards. This view is suited to inspect the the bilateral symmetry of the specimen.

### Image sources buttons

Adding an image source to BigDataViewer will also make it appear in the image **sources panel**, such as shown in below example.

<img width="220" alt="image" src="https://github.com/platybrowser/mmb-fiji/blob/master/images/74252332-d2740380-4ced-11ea-9a4e-d25cb1e83859.png">

To control the image sources appearance, there are several buttons...

- **[ C ]**  Change the color
- **[ B ]**  Change the brightness settings
- **[ X ]**  Remove from viewer and sources panel

...and checkboxes:

- **[ X ] V**  Check to show source in **v**olume viewer
- **[ X ] S**  Check to show source in **s**licing viewer (i.e., BigDataViewer) 

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

By default the PlatyBrowser plugin fetches the data from [github](https://github.com/platybrowser/platybrowser) and a publicly accessible object store. However it can also be configured to fetch the data from other locations, such as a local file-server. To access this functionality please type in Fiji’s search bar: "CustomBrowser". You can find a minimal example for setting up data for the MMB [here](https://github.com/platybrowser/mmb-python).


## Citation

If you use MMB, or the PlatyBrowser resource, please cite [Whole-body integration of gene expression and single-cell morphology](https://www.biorxiv.org/content/10.1101/2020.02.26.961037v1).

<!---
TODO we should also list important dependencies, FIJI, BDV, ?
--->
