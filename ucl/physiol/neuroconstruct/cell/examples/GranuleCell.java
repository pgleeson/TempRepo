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

import ucl.physiol.neuroconstruct.cell.*;

import javax.vecmath.*;
import ucl.physiol.neuroconstruct.cell.utils.*;

 /**
  *
  * A simple cell in the cerebellum, used for testing purposes
  *
  * @author Padraig Gleeson
  * @version 1.0.4
  *
  */


@SuppressWarnings("serial")

public class GranuleCell extends Cell
{
    Segment somaSegment = null;

    public GranuleCell()
    {
        this("GranuleCell");
    }


    public GranuleCell(String instanceName)
    {
        somaSegment = addSomaSegment(4, "Soma", null, null, new Section("Soma"));


      //  this.associateGroupWithChanMech(Section.SOMA_GROUP, "hh");
        setCellDescription("A very simplified cerebellar Granule like Cell for testing purposes only");
        setInstanceName(instanceName);

        this.createSections();

   //     this.associateGroupWithSynapse("parallel_fibers", "ExpSyn");
   //     this.associateGroupWithSynapse("dend_syn", "ExpSyn");
    }


    private void createSections()
    {
        float axonRadius = 0.6f;
        Point3f posnEndPoint = new Point3f(-0,140,-0);

        Segment mainAxon = addAxonalSegment(axonRadius, "mainAxon", posnEndPoint, somaSegment, 1, "mainAxonSection");


        Segment lastAxonalSectionPos = mainAxon;
        Segment lastAxonalSectionNeg = mainAxon;

        //Section parallelFibersSectionPos = new Section();
        //Section parallelFibersSectionNeg = new Section();




        for (int i = 0; i < 3; i++)
        {
            Segment temp1 = addRelativeAxon(lastAxonalSectionPos, new Point3f(40, 0, 0), "parallelFiberPos");
            lastAxonalSectionPos = temp1;


            Segment temp2 = addRelativeAxon(lastAxonalSectionNeg, new Point3f( -40, 0, 0), "parallelFiberNeg");
            lastAxonalSectionNeg = temp2;

            //temp1.getSection().addToGroup("parallel_fibers");
            //temp2.getSection().addToGroup("parallel_fibers");
        }
/*
       float dendriteRadius = 1.5f;

       posnEndPoint = new Point3f(0, -0.01f, 0);
       Segment rootDend = addDendriticSegment(dendriteRadius, "dend1", posnEndPoint, somaSegment, 0, "rootDendSeg");

       //Section sec1 = new Section("sec1");
       //Section sec2 = new Section("sec2");
       //Section sec3 = new Section("sec3");
       //Section sec4 = new Section("sec4");

       Segment d1 = addRelativeDendrite(rootDend, new Point3f(10, 0, 0), "sec1");
       Segment d2 = addRelativeDendrite(rootDend, new Point3f( -10, 0, 0), "sec2");
       Segment d3 = addRelativeDendrite(rootDend, new Point3f(0, 0, 10), "sec3");
       Segment d4 = addRelativeDendrite(rootDend, new Point3f(0, 0, -10), "sec4");

       Segment d5 = addRelativeDendrite(d1, new Point3f(0, -5, 0), d1.getSection().getSectionName());
       Segment d6 = addRelativeDendrite(d2, new Point3f(0, -5, 0), d2.getSection().getSectionName());
       Segment d7 = addRelativeDendrite(d3, new Point3f(0, -5, 0), d3.getSection().getSectionName());
       Segment d8 = addRelativeDendrite(d4, new Point3f(0, -5, 0), d4.getSection().getSectionName());

       d1.getSection().addToGroup("dend_syn");
       d2.getSection().addToGroup("dend_syn");
       d3.getSection().addToGroup("dend_syn");
       d4.getSection().addToGroup("dend_syn");*/


    }


    private Segment addRelativeAxon(Segment parent, Point3f relPosition, String sectionName)
    {
        Point3f newPosition = (new Point3f(parent.getEndPointPosition()));
        newPosition.add(relPosition);

        float newRadius = parent.getRadius();

        String newName = "Axon_"+ getOnlyAxonalSegments().size();

        Segment temp = addAxonalSegment(newRadius, newName,newPosition, parent, 1, sectionName);

        return temp;
    }

    public static void main(String[] args)
    {
        GranuleCell gc = new GranuleCell("Granny");

        System.out.println(CellTopologyHelper.printDetails(gc, null));

        System.out.println("Chans: "+ gc.getChanMechsVsGroups());
        System.out.println("Syns: "+ gc.getSynapsesVsGroups());
    }


}
