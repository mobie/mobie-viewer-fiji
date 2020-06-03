
# rename old versions of jars
#
for f in "*aws*" "bigdataviewer-core" "bigdataviewer-vistools" "imglib2" "n5" "n5-aws-s3" "n5-imglib2" "imagej-utils" "jackson-databind" "jackson-core" "jackson-annotations" "commons-compress" "lz4-java"
do
  find "$1/jars" \( -name $f"-[0-9]*" -a -not -name "*_backup*" \) -exec mv {} {}_mobie_backup \;
done

# copy new versions
#
cp ./jars/* $1/jars
