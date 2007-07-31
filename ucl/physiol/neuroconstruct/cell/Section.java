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

package ucl.physiol.neuroconstruct.cell;

import java.util.*;
import javax.vecmath.*;
import java.io.*;
import ucl.physiol.neuroconstruct.cell.examples.*;
import ucl.physiol.neuroconstruct.cell.utils.*;

/**
 *
 * A Section marks the beginning of a set of segments, and provides the start
 * point and start radius for the section. The subsequent points along the section
 * are provided by the segments. Note: a section is unbranched, the segments are
 * connected to the 1 end of their parents, and the segments are all in the same groups
 * and so will have the same biophysical characteristics (e.g. common synaptic types allowed)
 *
 * @author Padraig Gleeson
 * @version 1.0.4
 *
 */

public class Section
{
    public static final String ALL = "all";
    public static final String SOMA_GROUP = "soma_group";
    public static final String DENDRITIC_GROUP = "dendrite_group";
    public static final String AXONAL_GROUP = "axon_group";

    /**
     * String uniquely identifying the section
     */
    private String sectionName = new String();

    /**
     * Start point for section (and for first segment)
     */
    private Point3f startPointPosition = new Point3f(Float.NaN, Float.NaN, Float.NaN);


    /**
     * Starting radius for section (and for first segment)
     */
    private float startRadius = 0;

    /**
     * Groups to which ALL segments in this section belong
     */
    private Vector<String> groups = new Vector<String>();

    /**
     * This corresponds to nseg in NEURON
     */
    private int numberInternalDivisions = 1;


    /**
     * Added so that if anything funny happens in import, a comment can be added
     * to explain what's going on. No functional relevance, but can be printed in Cell Info
     * and included with MorphML export
     */
    private String comment = null;


    /**
     * This needs to be public for XMLEncoder. DON'T USE IT ON ITS OWN!
     */
    public Section()
    {
        if (!groups.contains("all")) groups.add("all");
    }

    /**
     * Standard constructor
     */
    public Section(String sectionName)
    {
        if (!groups.contains("all")) groups.add("all");
        this.sectionName = sectionName;
    }

    /**
     * Returns a string with the internal info on the section
     * @return String representation of section
     */
    public String toString()
    {
        String info = "Section: " + sectionName
            + ", init radius: " + startRadius
            + ", start: " + getStartPointPosition()
            + ", internal divs: " + numberInternalDivisions
            + ", groups: " + groups;
        if (comment!=null)  info = info +" // "+comment;

        return info;

    }

    /**
     * Returns a string with the internal info on the section
     * @return String representation of the section
     */
    public String toHTMLString(boolean includeTabs)
    {
        if (!includeTabs) return toString();

        String info =  "Section: <span style=\"color:green;font-weight:bold\">"+ sectionName
                + "</span>, init radius: " + startRadius
                + ", start: " +getStartPointPosition()
                + ", internal divs: " + numberInternalDivisions
                + ", groups: <b>"+ groups+ "</b>";

        if (comment!=null)  info = info +"<span style=\"color:#A9A9A9;font-weight:bold\">"+" // "+comment+"</span>";
            return info;
    }


    public Object clone()
    {
        Section newSection = new Section(sectionName);
        newSection.setStartRadius(startRadius);
        newSection.setStartPointPositionX(startPointPosition.x);
        newSection.setStartPointPositionY(startPointPosition.y);
        newSection.setStartPointPositionZ(startPointPosition.z);
        newSection.setGroups((Vector<String>)groups.clone());
        newSection.setNumberInternalDivisions(numberInternalDivisions);
        newSection.setComment(comment);

        return newSection;
    }

    public String getSectionName()
    {
        return sectionName;
    }

    public void setSectionName(String sectionName)
    {
        this.sectionName = sectionName;
    }

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String comment)
    {
        //this.edited = true;
        this.comment = comment;
    }

    // These funcs are needed as Point3f etc. aren't too compatible with
    // XMLEncoder, there aren't get/sets for x,y,z...



    public void setStartPointPositionX(float val)
    {
        this.startPointPosition.x = val;
    }

    public void setStartPointPositionY(float val)
    {
        this.startPointPosition.y = val;
    }

    public void setStartPointPositionZ(float val)
    {
        this.startPointPosition.z = val;
    }

    public float getStartPointPositionX()
    {
        return this.startPointPosition.x;
    }

    public float getStartPointPositionY()
    {
        return this.startPointPosition.y;
    }

    public float getStartPointPositionZ()
    {
        return this.startPointPosition.z;
    }


    public boolean equals(Object obj)
    {
        if (obj instanceof Section)
        {
            Section otherSection = (Section)obj;

            if (!otherSection.getSectionName().equals(sectionName)) // v quick check for most cases
                return false;

            // detailed checks
            if (((otherSection.getSectionName() == null && sectionName == null) ||
                otherSection.getSectionName().equals(sectionName)) &&

                ((otherSection.getStartPointPosition() == null && startPointPosition == null) ||
                 otherSection.getStartPointPosition().equals(startPointPosition)) &&

                otherSection.getNumberInternalDivisions() == numberInternalDivisions &&

                otherSection.getStartRadius() == startRadius &&

                /** @todo check comment... */

                groups.equals(otherSection.getGroups()))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * This corresponds to nseg in NEURON
     * @return number of internal divisions
     */
    public int getNumberInternalDivisions()
    {
        return numberInternalDivisions;
    }


    /**
     * This corresponds to nseg in NEURON
     * @param numberInternalDivisions how many internal divisions
     * should be used in NEURON simulations
     */
    public void setNumberInternalDivisions(int numberInternalDivisions)
    {
        this.numberInternalDivisions = numberInternalDivisions;
    }

    public Vector<String> getGroups()
    {
        return groups;
    }

    /**
     * Specifies the section (and so all the segments associated with it) to be in a particular group
     * Note: only one of SOMA_GROUP, DENDRITIC_GROUP, AXONAL_GROUP is allowed in the set of groups
     */
    public void addToGroup(String group)
    {
        if (group.equals(SOMA_GROUP))
        {
            groups.remove(DENDRITIC_GROUP);
            groups.remove(AXONAL_GROUP);
        }
        else if (group.equals(DENDRITIC_GROUP))
        {
            groups.remove(SOMA_GROUP);
            groups.remove(AXONAL_GROUP);
        }
        else if (group.equals(AXONAL_GROUP))
        {
            groups.remove(SOMA_GROUP);
            groups.remove(DENDRITIC_GROUP);
        }


        if (!groups.contains(group)) groups.add(group);
    }

    public void removeFromGroup(String group)
    {
        groups.remove(group);
    }

    public void setGroups(Vector<String> groups)
    {
        // ensure group all is here...
        if (!groups.contains("all")) groups.add("all");

        // to ensure it's not in 2 or more of SOMA_GROUP, DENDRITIC_GROUP, AXONAL_GROUP
        for (int i = 0; i < groups.size(); i++)
        {
           addToGroup((String)groups.elementAt(i));
        }
    }


    public boolean isSomaSection()
    {
        return this.groups.contains(SOMA_GROUP);
    }

    public Point3f getStartPointPosition()
    {
        return startPointPosition;
    }

    public float getStartRadius()
    {
        return startRadius;
    }

    public void setStartRadius(float startRadius)
    {
        this.startRadius = startRadius;
    }



    public static void main(String[] args)
    {/*
        Section s1 = new Section("s1");
        System.out.println("s1: "+ s1);

        Section s1_ = new Section("s1");
        System.out.println("s1_: "+ s1_);

        System.out.println("Equal: "+ s1.equals(s1_));

        s1.addToGroup(SOMA_GROUP);

        System.out.println("Groups: "+ s1.getGroups());

        s1.addToGroup(DENDRITIC_GROUP);
        Vector v = new Vector();
        v.add(SOMA_GROUP);
        v.add(DENDRITIC_GROUP);
        s1.setGroups(v);

        System.out.println("Groups: "+ s1);

        s1.setStartPointPosition(new Point3f(0,9,9));
        s1.setStartRadius(66);




        try
        {
            File f = new File("c:\\temp\\cell.xml");
            FileOutputStream fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            XMLEncoder xmlEncoder = new XMLEncoder(bos);

            xmlEncoder.writeObject(s1);

            xmlEncoder.flush();
            xmlEncoder.close();

            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            XMLDecoder xmlDecoder = new XMLDecoder(bis);


             Object obj = xmlDecoder.readObject();
             System.out.println("Obj: "+ obj);

        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
            return;
        }



        Hashtable h = new Hashtable();
        Segment oldSeg = new Segment();;
             oldSeg.setSegmentName("Seg_");

        for (int i = 0; i < 200; i++)
        {
             Segment seg = new Segment();
             seg.setSegmentName("Seg_"+i);
             seg.setParentSegment(oldSeg);
             h.put(seg.getSegmentName(), seg);
             oldSeg = seg;

             System.out.println("Added seg: "+ seg.getSegmentName()
                                +", parent: "+ seg.getParentSegment().getSegmentName());
        }

*/
     Section s1 = new Section("s1");
      s1.setStartPointPositionX(0);
      s1.setStartPointPositionY(0);
      s1.setStartPointPositionZ(0);
      System.out.println("s1: " + s1);

      s1.getGroups().add("hdghkg");

      Section s2 = null;

      s2 = (Section) s1.clone();
      Vector<String> newGroups = new Vector<String>();
      newGroups.add("hdghkg");
      s2.setGroups(newGroups);

   //   s2.setStartPointPositionX(333);

      System.out.println("s2: " + s2);
      System.out.println("s1: " + s1);
      System.out.println("s1 = s2? : " + s1.equals(s2));







      File f = new File("../temp/cde.tmp");

      SimpleCell cell = new SimpleCell("hh");
      Cell orig = cell;
      Cell reloaded =  null;

      System.out.println("Original: " + CellTopologyHelper.printDetails(cell, null) );

      try
      {
          System.out.println("Chucking it into: " + f.getCanonicalPath());
          System.out.println("Created: " + f.createNewFile());

          FileOutputStream fo = new FileOutputStream(f);
          ObjectOutputStream so = new ObjectOutputStream(fo);
          so.writeObject(orig);
          so.flush();
          so.close();
      }
      catch (Exception e)
      {
          e.printStackTrace();
          System.exit(1);
      }

      // Deserialize in to new class object
      try
      {
          FileInputStream fi = new FileInputStream(f);
          ObjectInputStream si = new ObjectInputStream(fi);
          reloaded = (Cell)si.readObject();

          System.out.println("Created: " + CellTopologyHelper.printDetails(reloaded, null));

          System.out.println("Compare: " + CellTopologyHelper.compare(reloaded, orig));

          System.out.println("Equal: " + orig.equals(reloaded));

          si.close();
      }
      catch (Exception e)
      {
          e.printStackTrace();
          System.exit(1);
        }



    }




}




