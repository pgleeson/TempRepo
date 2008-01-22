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

import java.io.Serializable;
import java.util.*;
import javax.vecmath.*;

import ucl.physiol.neuroconstruct.cell.examples.*;
import ucl.physiol.neuroconstruct.j3D.*;
import ucl.physiol.neuroconstruct.utils.compartment.*;
import ucl.physiol.neuroconstruct.utils.ClassLogger;

/**
 *
 * A Segment which is the basic unit of somas, dendrites and axons. Segments belong
 * to Sections, with the Section providing the start point and radius, and the segments
 * specifying the points along a section
 *
 * @author Padraig Gleeson
 *  
 *
 */


public class Segment implements Serializable
{
    static final long serialVersionUID = 6877533563271791697L;
    
    private transient ClassLogger logger = new ClassLogger("Segment");

    /**
     * Name given to Segment, unique within a cell
     */
    private String segmentName = null;

    /**
     * Reference to parent Segment
     */
    private Segment parentSegment = null;


    /**
     * Unique (for this Cell) Id for the Segment. They should all be >=0 and ideally ascending
     * This is given a funny initial value to ensure the actual value is written to the
     * XML file generated by XMLEncoder
     */
    private int segmentId = Integer.MAX_VALUE;


    /**
     * Fraction along parent *Segment*, NOT fraction along parent section, as a statement like
     * 'soma connect dend1[0] (0), 0.5' in NEURON would mean
     */
    private float fractionAlongParent = 1;

    /**
     * Section to which this Segment belongs
     */
    private Section section = null;

    /**
     * Whether the volume of this segment should be taken into account for packing...
     */
    private boolean finiteVolume = false;

    /**
     * Added so that if anything funny happens in import, a comment can be added
     * to explain what's going on. No functional relevance, but can be printed in Cell Info
     * and included with MorphML export
     */
    private String comment = null;

    public static int UNDETERMINED_SHAPE = -1;
    public static int SPHERICAL_SHAPE = 0;
    public static int CYLINDRICAL_SHAPE = 1;


    // This is set so, to ensure some values (especially 0) are put into the xml
    // representation of endPointPosition. If new Point3f() was used, XMLEncoder
    // wouldn't generate x, y, z entries in XML if x=0, etc.
    private Point3f endPointPosition = new Point3f(Float.NaN, Float.NaN, Float.NaN);


    /**
     * Radius of the end point of the Segment.
     * This is given a funny initial value to ensure the actual value is written to the
     * XML file generated by XMLEncoder
     */
    private float radius = Float.MAX_VALUE;

    /**
     * Flag to say the segment/cell has been altered after loading. When project is saved only edited cells will
     * have their morphologies re saved. Note this is not foolproof as the
     */
    private boolean edited = false;


    /**
     * This needs to be public for XMLEncoder. DON'T USE IT ON ITS OWN!
     */
    public Segment()
    {
    }



    /**
     * For adding a soma, axon or dendrite segment
     */
    protected Segment(String segmentName,
                   float radius,
                   Point3f endPosition,
                   int segmentId,
                   Segment parentSegment,
                   float fractionAlongParent,
                   Section section)
    {
        if (fractionAlongParent<0) fractionAlongParent=0;
        if (fractionAlongParent>1) fractionAlongParent=1;

        this.fractionAlongParent = fractionAlongParent;
        this.segmentName = segmentName;
        this.radius = radius;
        this.endPointPosition = endPosition;
        if (endPointPosition==null) endPointPosition = new Point3f();

      //  if (parentSegment==null && endPointPosition.equals(new Point3f()))
      //      this.shape = SPHERICAL_SHAPE;
/*
        if (parentSegment == null ||
            !parentSegment.getSection().getSectionName().equals(section.getSectionName()))
        {
            // start of new section...
            firstSectionSegment = true;
        }
*/
        this.parentSegment = parentSegment;
        this.segmentId = segmentId;
        this.section = section;
    }

    /**
     * Note: This produced a clone of the Segment with most of the info intact
     * but DOESN'T clone the **parentSegment or Section**. These will be null and need to
     * be set outside this function. Shouldn't be used to create a new Segment for the existing
     * Cell but as part of cloning the whole cell!!
     * @see Cell#clone()
     */
    public Object clone()
    {
        Segment newSegment = new Segment();

        newSegment.setSegmentName(segmentName);
        newSegment.setSegmentId(segmentId);
        newSegment.setParentSegment(null);
        newSegment.setSection(null);
        newSegment.setFractionAlongParent(fractionAlongParent);
        newSegment.setFiniteVolume(finiteVolume);
        newSegment.setRadius(radius);
        newSegment.setEndPointPositionX(endPointPosition.x);
        newSegment.setEndPointPositionY(endPointPosition.y);
        newSegment.setEndPointPositionZ(endPointPosition.z);
        newSegment.setComment(comment);
        // DOESN'T clone the **parentSegment or Section**

        return newSegment;
    }


    public Point3f getEndPointPosition()
    {
        return this.endPointPosition;
    }


    public Point3f getStartPointPosition()
    {
        if (this.isFirstSectionSegment())
        {
            if (section==null) return null;
            //System.out.println("sec: "+section);
            return section.getStartPointPosition();
        }

        /** @todo remove... */
        if (parentSegment.getSegmentId() == getSegmentId())
        {
            logger.logError("parentSegment: " + parentSegment+" and "+this +" have same id, so cannot getStartPointPosition()!!");
            return null;
        }

        if (parentSegment == null) return new Point3f(Float.NaN, Float.NaN, Float.NaN);

        Point3f startPoint = new Point3f();

        /**
         * This single line is the result of about 3 days debugging...
         */
        if (fractionAlongParent==1) return parentSegment.getEndPointPosition();


        startPoint.x = ( (1f - fractionAlongParent) * parentSegment.getStartPointPosition().x)
            + (fractionAlongParent * parentSegment.getEndPointPosition().x);
        startPoint.y = ( (1f - fractionAlongParent) * parentSegment.getStartPointPosition().y)
            + (fractionAlongParent * parentSegment.getEndPointPosition().y);
        startPoint.z = ( (1f - fractionAlongParent) * parentSegment.getStartPointPosition().z)
            + (fractionAlongParent * parentSegment.getEndPointPosition().z);

        return startPoint;

    }


    public float getSegmentStartRadius()
    {
        if (this.isFirstSectionSegment())
        {
            return section.getStartRadius();
        }

        if (parentSegment == null) return Float.NaN;

        return parentSegment.getRadius();

    }


    public float getSegmentSurfaceArea()
    {
        if (getStartPointPosition()==null)
        {
            return Float.NaN;
        }
        if (getSegmentShape()==SPHERICAL_SHAPE)
        {
            return 4 * (float)Math.PI * getSegmentStartRadius() * getSegmentStartRadius();
        }

        SimpleCompartment comp = new SimpleCompartment(getSegmentStartRadius(),
                                                       getRadius(),
                                                       getSegmentLength());

        return (float)comp.getCurvedSurfaceArea();
    }


    public boolean isEdited()
    {
        return this.edited;
    }

    /**
     * Can be used after loading cell
     */
    public void setAsUnedited()
    {
        this.edited = false;
    }


    public float getSegmentLength()
    {
        if (getStartPointPosition()==null)
        {
            return Float.NaN;
        }
        return endPointPosition.distance(getStartPointPosition());
    }


    public float getSegmentVolume()
    {
        if (getStartPointPosition()==null)
        {
            return Float.NaN;
        }
        if (this.isSpherical())
            return (float)((4/3f)*Math.PI*getRadius()*getRadius()*getRadius());

        return (float)(new SimpleCompartment(this)).getVolume();
    }




    public Segment getParentSegment()
    {
        return this.parentSegment;
    }




    /**
     * Gets the radius at the END of the Segment
     */
    public float getRadius()
    {
        return radius;
    }

    public void setRadius(float radius)
    {
        this.radius = radius;
    }


    public boolean isAxonalSegment()
    {
        if (section.getGroups().contains(Section.AXONAL_GROUP)) return true;
        return false;
    }

    public boolean isDendriticSegment()
    {
        if (section.getGroups().contains(Section.DENDRITIC_GROUP)) return true;
        return false;
    }

    public boolean isSomaSegment()
    {
        if (section==null) return false;
        if (section.getGroups().contains(Section.SOMA_GROUP)) return true;
        return false;
    }

    public boolean isRootSegment()
    {
        return isSomaSegment() && isFirstSectionSegment();
    }


    public String toShortString()
    {

        return segmentName +" (ID: "+segmentId+")";
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(segmentName);

        if (section == null) sb.append(", -- No section specified --");
        else sb.append(", section: " + section.getSectionName());

        if (getSegmentShape()==SPHERICAL_SHAPE) sb.append(", SPHERICAL");
        if (getSegmentShape()==UNDETERMINED_SHAPE) sb.append(", **UNDETERMINED SHAPE**");
        sb.append(", ID: " + segmentId );

        if(this.isFirstSectionSegment() && parentSegment == null)
        {
            sb.append(", ROOT SEGMENT");
        }
        else
        {
            if (parentSegment == null) sb.append(", -- NO PARENT --");
            else
            {
                sb.append(", parent: " + parentSegment.getSegmentName()+" ("+parentSegment.getSegmentId()+")");
                if (fractionAlongParent!=1)
                sb.append(", FRACT ALONG: " + fractionAlongParent);

            }
        }

        sb.append(", rad: " + radius);

        if (getStartPointPosition() == null)
            sb.append(", (-- NO PARENT --)");
        else
            sb.append(", " + getStartPointPosition());

        sb.append(" -> "+ getEndPointPosition());

        if (getSegmentShape()!=SPHERICAL_SHAPE) sb.append(", len: "+ Utils3D.trimDouble(this.getSegmentLength(), 5));

        if (isFiniteVolume()) sb.append(" (FINITE VOLUME)");

        if (comment!=null)  sb.append(" // "+comment);

        return sb.toString();

    }



    public String toHTMLString(boolean includeTabs)
    {
        if (!includeTabs) return toString();

        StringBuffer sb = new StringBuffer();
        sb.append("<span style=\"color:blue;font-weight:bold\">"+segmentName+"</span>");

        if (section == null) sb.append(", -- No section specified --");
        else sb.append(", section: " + section.getSectionName());

        if (getSegmentShape()==SPHERICAL_SHAPE) sb.append(", SPHERICAL");
        sb.append(", ID: " + segmentId );

        if(this.isFirstSectionSegment() && isSomaSegment()) sb.append(", ROOT SEGMENT");
        else
        {
            if (parentSegment == null) sb.append(", -- NO PARENT --");
            else
            {
                sb.append(", parent: " + parentSegment.getSegmentName());
                if (fractionAlongParent!=1)
                sb.append(", FRACT ALONG: " + fractionAlongParent);

            }
        }

        sb.append(", rad: " + radius);

        if (getStartPointPosition() == null)
            sb.append(", (-- NO PARENT --)");
        else
            sb.append(", " + getStartPointPosition());

        sb.append(" -> "+ getEndPointPosition());

        if (getSegmentShape()!=SPHERICAL_SHAPE) sb.append(", len: "+ Utils3D.trimDouble(this.getSegmentLength(), 5));

        if (isFiniteVolume()) sb.append(" (FINITE VOLUME)");

        if (comment!=null)  sb.append("<span style=\"color:#A9A9A9;font-weight:bold\">"+" // "+comment+"</span>");

        return sb.toString();

    }



    public Vector<String> getGroups()
    {
        return section.getGroups();
    }



    public static void main(String[] args)
    {
        Section sec = new Section("sec1");
        sec.addToGroup(Section.SOMA_GROUP);
        sec.setStartPointPositionX(0);
        sec.setStartPointPositionY(0);
        sec.setStartPointPositionZ(0);

        System.out.println("Section: "+sec);

        Segment s1 = new Segment("s1", 111, new Point3f(1,1,1), 0, null, 1, sec);

        Segment s2 = new Segment("s2", 222, new Point3f(2,2,2), 222, s1, 1, sec);

        System.out.println("s1: "+ s1);
        System.out.println("s2: "+ s2);
        System.out.println("s2 section: "+ s2.getSection());

        Segment s3 = null;
        try
        {
            s3 = (Segment) s2.clone();

            System.out.println("s3: "+ s3);
            System.out.println("s3 section: "+ s3.getSection());

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        try
        {
            Cell cell = new ComplexCell("Copm");

            System.out.println("Cell: " + cell);

            int id = 3;

            Segment aSeg = cell.getSegmentWithId(id);

            aSeg.setComment("created...");

            System.out.println("Orig seg :" + aSeg);

            Cell cCell = (Cell) cell.clone();

            System.out.println("New Cell: " + cCell);

            Segment cSeg = cCell.getSegmentWithId(id);

            System.out.println("New seg :" + aSeg);

            System.out.println("Equals: " + aSeg.fullEquals(cSeg));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }


    }


    public String getSegmentName()
    {
        return this.segmentName;
    }

    public String getComment()
    {
        return this.comment;
    }


    public boolean isFiniteVolume()
    {
        return finiteVolume;
    }

    public void setFiniteVolume(boolean finiteVolume)
    {
        this.edited = true;
        this.finiteVolume = finiteVolume;
    }



    public void setSegmentName(String segmentName)
    {
        this.edited = true;
        if (segmentName != null && segmentName.trim().length() > 0)
            this.segmentName = segmentName;
    }

    public void setComment(String comment)
    {
        this.edited = true;
        this.comment = comment;
    }




    /**
     * These funcs are needed as Point3f etc. aren't too compatible with
     * XMLEncoder, there aren't get/sets for x,y,z...
     */
    public void setEndPointPositionX(float val)
    {
        this.edited = true;
        this.endPointPosition.x = val;
    }

    public void setEndPointPositionY(float val)
    {
        this.edited = true;
        this.endPointPosition.y = val;
    }

    public void setEndPointPositionZ(float val)
    {
        this.edited = true;
        this.endPointPosition.z = val;
    }

    public float getEndPointPositionX()
    {
        return this.endPointPosition.x;
    }

    public float getEndPointPositionY()
    {
        return this.endPointPosition.y;
    }

    public float getEndPointPositionZ()
    {
        return this.endPointPosition.z;
    }

    public void setParentSegment(Segment parentSegment)
    {
        this.parentSegment = parentSegment;
    }

    public int getSegmentId()
    {
        return segmentId;
    }
    public void setSegmentId(int segmentId)
    {
        this.edited = true;
        this.segmentId = segmentId;
    }


    public boolean isFirstSectionSegment()
    {
        if (parentSegment == null ||
            !parentSegment.getSection().equals(section))
        {
            return true;
        }
        return false;
    }

    /**
     * This equals function is used when calling CellTopologyHelper.compare( , )
     */
    public boolean fullEquals(Object obj)
    {
        if (!equals(obj)) return false;
        Segment other = (Segment)obj;

        if (getSection()!=null && other.getSection()!=null)
        {
            if (!getSection().getSectionName().equals(other.getSection().getSectionName()))
            {
                logger.logComment("Sections do not match");
                return false;
            }
        }

        if (getParentSegment() != null && other.getParentSegment() != null)
        {
            if (getParentSegment().getSegmentId() != other.getParentSegment().getSegmentId())
            {
                logger.logComment("Parent Sections do not match");
                return false;
            }
        }


        return true;
    }

    /*
    * Note the equals function is mainly used during the cloning of cells, when the
    * parent segment and section aren't set. therefore the check on identity of parents isn't made here!
    *
    */
    public boolean equals(Object obj)
    {
        if (obj instanceof Segment)
        {
            Segment other = (Segment)obj;

            if (!other.getSegmentName().equals(this.segmentName)) return false;

            /*
            * Note the equals function is mainly used during the cloning of cells, when the
            * parent segment and section aren't set. therefore the check on identity of parents isn't made here!
            *

            if (parentSegment==null)
            {
                if (other.getParentSegment()!=null)  return false;
            }
            else
            {
                if (!other.getParentSegment().equals(parentSegment))  return false;
            }

            if (!other.getSection().equals(section)) return false;

            */

            if (other.getSegmentId()!= this.segmentId) return false;

            if (other.getFractionAlongParent()!= this.fractionAlongParent) return false;


            if (other.isFiniteVolume()!= this.finiteVolume) return false;

            if (comment==null)
            {
                if (other.getComment()!=null)  return false; // not important but throw it in anyway for completeness
            }
            else
            {
                if (!other.getComment().equals(comment))  return false;
            }

            if (!other.getEndPointPosition().equals(endPointPosition)) return false;

            if (other.getRadius()!= this.radius) return false;

            return true;
        }
        return false;
    }


    public int getSegmentShape()
    {
        Point3f startPoint = getStartPointPosition();
        if (startPoint==null)
        {
            return UNDETERMINED_SHAPE;
        }
        if (this.getEndPointPosition().equals(startPoint))
            return Segment.SPHERICAL_SHAPE; // by definition...
        else
            return Segment.CYLINDRICAL_SHAPE;
    }

    public boolean isSpherical()
    {
        return (getSegmentShape() == Segment.SPHERICAL_SHAPE);
    }

    public Section getSection()
    {
        return section;
    }
    public void setSection(Section section)
    {
        this.edited = true;
        this.section = section;
    }
    public float getFractionAlongParent()
    {
        return fractionAlongParent;
    }
    public void setFractionAlongParent(float fractionAlongParent)
    {
        this.edited = true;
        this.fractionAlongParent = fractionAlongParent;
    }


}
