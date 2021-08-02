package de.embl.cba.mobie.n5.zarr;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.XmlIoSequenceDescription;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.InputStream;

public class CustomZarrXmlIoSpimData extends XmlIoAbstractSpimData<SequenceDescription, SpimData>
{
    public CustomZarrXmlIoSpimData() {
        super(SpimData.class, new XmlIoSequenceDescription(), new XmlIoViewRegistrations());
    }

    public SpimData loadFromStream( InputStream in, String xmlFilename) throws SpimDataException
    {
        SAXBuilder sax = new SAXBuilder();

        Document doc;
        try {
            doc = sax.build(in);
        } catch (Exception var6) {
            throw new SpimDataIOException(var6);
        }

        Element root = doc.getRootElement();
        if (root.getName() != "SpimData") {
            throw new RuntimeException("expected <SpimData> root element. wrong file?");
        } else {
            return (SpimData)this.fromXml(root, new File(xmlFilename));
        }
    }
}

