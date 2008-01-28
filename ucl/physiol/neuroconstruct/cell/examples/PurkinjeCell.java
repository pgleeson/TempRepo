/**
 * neuroConstruct
 *
 * Software for developing large scale 3D networks of biologically realistic neurons
 * Copyright (c) 2008 Padraig Gleeson
 * UCL Department of Physiology
 *
 * Development of this software was made possible with funding from the
 * Medical Research Council
 *
 */

package ucl.physiol.neuroconstruct.cell.examples;

import ucl.physiol.neuroconstruct.cell.*;


import javax.vecmath.*;
import java.util.*;

/**
 *
 * A simple cell in the cerebellum, used for testing purposes
 *
 * @author Padraig Gleeson
 *  
 *
 */


@SuppressWarnings("serial")

public class PurkinjeCell extends Cell
{

    Segment somaSection = null;

    public PurkinjeCell()
    {
        this("PurkinjeCell");
    }


    public PurkinjeCell(String instanceName)
    {
        somaSection = addFirstSomaSegment(15,15, "Soma", new Point3f(0,0,0), new Point3f(0,0,0), new Section("Soma"));
   //     this.associateGroupWithChanMech(Section.SOMA_GROUP, "hh");
        setCellDescription("A very simplified Purkinje Cell for testing purposes only");
        setInstanceName(instanceName);

        this.createSections();

     //   this.associateGroupWithSynapse("main_dends", "ExpSyn");

    }


    private void createSections()
    {
        float axonRadius = 1.0f;

        Point3f posnEndPoint = new Point3f(-0,-240,-0);
        Segment mainAxon = addAxonalSegment(axonRadius, "mainAxon", posnEndPoint,  somaSection,0, "AxonSec");
        mainAxon.getSection().setStartRadius(axonRadius);

        posnEndPoint = new Point3f(-5,-260,-0);
        addAxonalSegment(axonRadius, "axon1", posnEndPoint, mainAxon, 1, "AxonSec1");

        posnEndPoint = new Point3f(5,-260,-0);
        addAxonalSegment(axonRadius, "axon2", posnEndPoint, mainAxon, 1, "AxonSec2");


        float dendriteRadius = 1.5f;

        posnEndPoint = new Point3f(0,30,0);
        Segment rootDend = addDendriticSegment(dendriteRadius, "root",posnEndPoint, somaSection,1, "DendRootSec", false);
        rootDend.getSection().setStartRadius(dendriteRadius);

        posnEndPoint = new Point3f(0,50,0);
        Segment mainDend1 = addDendriticSegment(dendriteRadius, "mainDend1",posnEndPoint, rootDend,1, "DendSec1", true);

        posnEndPoint = new Point3f(0,50,-40);

        Segment mainDend2 = addDendriticSegment(dendriteRadius, "mainDend2",posnEndPoint, rootDend,1, "DendSec2", true);

        posnEndPoint = new Point3f(0,50,40);

        Segment mainDend3 = addDendriticSegment(dendriteRadius, "mainDend3",posnEndPoint, rootDend,1, "DendSec3", true);


        Vector<Segment> secondLayer = new Vector<Segment>();

        secondLayer.add(addRelativeDendrite(mainDend1, new Point3f(0,30,-10)));
        secondLayer.add(addRelativeDendrite(mainDend1, new Point3f(0,30,10)));
        secondLayer.add(addRelativeDendrite(mainDend1, new Point3f(0,35,0)));

        secondLayer.add(addRelativeDendrite(mainDend2, new Point3f(0,20,-10)));
        secondLayer.add(addRelativeDendrite(mainDend2, new Point3f(0,20,10)));
        secondLayer.add(addRelativeDendrite(mainDend2, new Point3f(0,25,0)));

        secondLayer.add(addRelativeDendrite(mainDend3, new Point3f(0,20,-10)));
        secondLayer.add(addRelativeDendrite(mainDend3, new Point3f(0,20,10)));
        secondLayer.add(addRelativeDendrite(mainDend3, new Point3f(0,25,0)));

        for (int i = 0; i < secondLayer.size(); i++)
        {
            Segment dend = (Segment)secondLayer.elementAt(i);
            Segment d1 = addRelativeDendrite(dend, new Point3f(5,50,-14));
            Segment d2 = addRelativeDendrite(dend, new Point3f(0,50,0));
            Segment d3 = addRelativeDendrite(dend, new Point3f(-5,50,14));

            d1.getSection().addToGroup("main_dends");
            d2.getSection().addToGroup("main_dends");
            d3.getSection().addToGroup("main_dends");
        }



    }

    private Segment addRelativeDendrite(Segment parent, Point3f relPosition)
    {
        Point3f newPosition = (new Point3f(parent.getEndPointPosition()));
        newPosition.add(relPosition);

        float newRadius = parent.getRadius()*.6f;

        String newName = "Dend_"+ getOnlyDendriticSegments().size();

        Segment tempDend = addDendriticSegment(newRadius, newName,newPosition, parent, 1, newName+"_Sec", true);

        return tempDend;
    }

}
