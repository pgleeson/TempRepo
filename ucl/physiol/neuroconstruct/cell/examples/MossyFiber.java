/**
 * neuroConstruct
 *
 * Software for developing large scale 3D networks of biologically realistic neurons
 * Copyright (c) 2007 Padraig Gleeson
 * UCL Department of Physiology
 *
 * Development of this software was made possible with funding from the
 * Medical Research Council
 *
 */

package ucl.physiol.neuroconstruct.cell.examples;

import javax.vecmath.*;
import ucl.physiol.neuroconstruct.cell.*;


 /**
  *
  * A simple cell in the cerebellum, used for testing purposes
  *
  * @author Padraig Gleeson
  * @version 1.0.4
  *
  */




public class MossyFiber extends Cell
{

    Segment somaSection = null;

    public MossyFiber()
    {
        this("MossyFiber");
    }


    public MossyFiber(String instanceName)
    {
        somaSection = addSomaSegment(8, "Soma", null, null, new Section("Soma"));

    //    this.associateGroupWithChanMech(Section.SOMA_GROUP, "hh");
        setCellDescription("A simple cell for testing purposes only");
        setInstanceName(instanceName);

        this.createSections();

      //  this.associateGroupWithSynapse("rosette", "ExpSyn");
    }


    private void createSections()
    {
        float axonRadius = 1;

        float rosetteRadius = 5;//temp...


        Point3f posnEndPoint = new Point3f(5,50,0);
        Segment rootAxon = addAxonalSegment(axonRadius, "ax_root", posnEndPoint, somaSection, 1, "axonSec");


        Segment branch1 = addRelativeAxon(rootAxon, new Point3f(-5,50,0), axonRadius, "mainAxon");




        Segment rosette = addRelativeAxon(branch1, new Point3f(0, 20, 0),
            rosetteRadius, "Rosette");
        rosette.setFiniteVolume(true);

        rosette.getSection().setStartRadius(rosetteRadius);

        rosette.getSection().addToGroup("rosette");

        float dendriteDiam = 1f;

        posnEndPoint = new Point3f(2,-20,0);
        Segment rootDend = addDendriticSegment(dendriteDiam, "dend_root",posnEndPoint, somaSection, 0, "dendSec");

        Point3f posnEndPoint2 = new Point3f(-2,-20,0);
        addRelativeDendrite(rootDend, posnEndPoint2);

    }

    private Segment addRelativeDendrite(Segment parent, Point3f relPosition)
    {
        Point3f newPosition = (new Point3f(parent.getEndPointPosition()));
        newPosition.add(relPosition);

        float newRadius = parent.getRadius()*.6f;

        String newName = "Dend_"+ getOnlyDendriticSegments().size();

        Segment tempDend = addDendriticSegment(newRadius, newName,newPosition, parent, 1, "dendSec");

        return tempDend;
    }


    private Segment addRelativeAxon(Segment parent, Point3f relPosition, float radius, String sectionName)
    {
        Point3f newPosition = (new Point3f(parent.getEndPointPosition()));
        newPosition.add(relPosition);

        String newName = "Axon_"+ getOnlyAxonalSegments().size();

        Segment tempAxon = addAxonalSegment(radius, newName,newPosition, parent, 1, sectionName);

        return tempAxon;
    }


}
