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
import ucl.physiol.neuroconstruct.cell.utils.*;
import ucl.physiol.neuroconstruct.utils.*;

 /**
  * A simple cell for test purposes.
  *
  * @author Padraig Gleeson
  * @version 1.0.3
  *
  */



public class OneSegment extends Cell
{
    static ClassLogger logger = new ClassLogger("OneSegment");

    Segment somaSection = null;

    public OneSegment()
    {
        //this("SimpleCell");
    }


    public OneSegment(String instanceName)
    {

        somaSection = addFirstSomaSegment(10,10, "Soma", null, null, new Section("Soma"));

        logger.logComment("Created soma: "+ somaSection);





        setCellDescription("A single segment/compartment cell");

        setInstanceName(instanceName);

    }



    public static void main(String[] args)
    {
        OneSegment cell = new OneSegment("I'm simple...");

        String group = "newgroup";
        String chan = "newchan";

        cell.getFirstSomaSegment().getSection().addToGroup(group);
        cell.getFirstSomaSegment().getSection().addToGroup(group+"*");


           ChannelMechanism chMech = new ChannelMechanism( "ggg", 22);
           cell.associateGroupWithChanMech(group, chMech);
        cell.associateGroupWithSynapse(group+"*", chan+"_syn");



        System.out.println(CellTopologyHelper.printDetails(cell, null));

        Cell newCell = (Cell)cell.clone();

    }


}
