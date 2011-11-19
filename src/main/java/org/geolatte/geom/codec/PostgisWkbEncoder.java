/*
 * This file is part of the GeoLatte project.
 *
 *     GeoLatte is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     GeoLatte is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with GeoLatte.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010 - 2011 and Ownership of code is shared by:
 * Qmino bvba - Romeinsestraat 18 - 3001 Heverlee  (http://www.qmino.com)
 * Geovise bvba - Generaal Eisenhowerlei 9 - 2140 Antwerpen (http://www.geovise.com)
 */

package org.geolatte.geom.codec;


import org.geolatte.geom.DimensionalFlag;
import org.geolatte.geom.*;

/**
 * A WKBEncoder for the PostGIS EWKB dialect (versions 1.0 to 1.5).
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Nov 11, 2010
 */
public class PostgisWkbEncoder {


    public ByteBuffer encode(Geometry geom, WkbByteOrder wbo) {
        ByteBuffer output = ByteBuffer.allocate(calculateSize(geom, true));
        if (wbo != null) {
            output.setWKBByteOrder(wbo);
        }
        writeGeometry(geom, output);
        output.rewind();
        return output;
    }

    private void writeGeometry(Geometry geom, ByteBuffer output) {
        geom.accept(new WkbVisitor(output));
    }

    private int calculateSize(Geometry geom, boolean includeSRID) {
        int size = 1 + ByteBuffer.UINT_SIZE; //size for order byte + type field
        if (geom.getSRID() > 0 && includeSRID) size += 4;
        if (geom instanceof GeometryCollection) {
            size += sizeOfGeometryCollection((GeometryCollection) geom);
        } else if (geom instanceof Polygon) {
            size += getPolygonSize((Polygon) geom);
        } else if (geom instanceof Point) {
            size += getPointByteSize(geom);
        } else if (geom instanceof PolyHedralSurface) {
            size += getPolyHedralSurfaceSize((PolyHedralSurface) geom);
        } else {
            size += ByteBuffer.UINT_SIZE; //to hold number of points
            size += getPointByteSize(geom) * geom.getNumPoints();
        }
        return size;
    }

    private int getPointByteSize(Geometry geom) {
        return geom.getCoordinateDimension() * ByteBuffer.DOUBLE_SIZE;
    }

    private int getPolyHedralSurfaceSize(PolyHedralSurface geom) {
        int size = ByteBuffer.UINT_SIZE;
        for (int i = 0; i < geom.getNumPatches(); i++) {
            size += getPolygonSize(geom.getPatchN(i));
        }
        return size;
    }

    private int getPolygonSize(Polygon geom) {
        //to hold the number of linear rings
        int size = ByteBuffer.UINT_SIZE;
        //for each linear ring, a UINT holds the number of points
        size += geom.isEmpty() ? 0 : ByteBuffer.UINT_SIZE * (geom.getNumInteriorRing() + 1);
        size += getPointByteSize(geom) * geom.getNumPoints();
        return size;
    }

    private int sizeOfGeometryCollection(GeometryCollection collection) {
        int size = ByteBuffer.UINT_SIZE;
        for (Geometry g : collection) {
            size += calculateSize(g, false);
        }
        return size;
    }

    private static class WkbVisitor implements GeometryVisitor {

    private final ByteBuffer output;
    private boolean hasWrittenSRID = false;

    WkbVisitor(ByteBuffer byteBuffer) {
        this.output = byteBuffer;
    }

    @Override
    public void visit(Point geom) {
        writeByteOrder(output);
        DimensionalFlag dimension = DimensionalFlag.parse(geom.is3D(), geom.isMeasured());
        writeTypeCodeAndSRID(geom, dimension, output);
        writePoints(geom.getPoints(), geom.getCoordinateDimension(), output);
    }

    @Override
    public void visit(LineString geom) {
        writeByteOrder(output);
        DimensionalFlag dimension = DimensionalFlag.parse(geom.is3D(), geom.isMeasured());
        writeTypeCodeAndSRID(geom, dimension, output);
        output.putUInt(geom.getNumPoints());
        writePoints(geom.getPoints(), geom.getCoordinateDimension(), output);
    }

    @Override
    public void visit(Polygon geom) {
        writeByteOrder(output);
        DimensionalFlag dimension = DimensionalFlag.parse(geom.is3D(), geom.isMeasured());
        writeTypeCodeAndSRID(geom, dimension, output);
        writeNumRings(geom, output);
        for (LinearRing ring : geom) {
            writeRing(ring);
        }
    }

    @Override
    public void visit(PolyHedralSurface geom) {
        writeByteOrder(output);
        DimensionalFlag dimension = DimensionalFlag.parse(geom.is3D(), geom.isMeasured());
        writeTypeCodeAndSRID(geom, dimension, output);
        output.putUInt(geom.getNumPatches());
        for (Polygon pg : geom) {
            pg.accept(this);
        }
    }

    @Override
    public void visit(GeometryCollection geom) {
        writeByteOrder(output);
        DimensionalFlag dimension = DimensionalFlag.parse(geom.is3D(), geom.isMeasured());
        writeTypeCodeAndSRID(geom, dimension, output);
        output.putUInt(geom.getNumGeometries());
        for (Geometry part : geom) {
            part.accept(this);
        }
    }

    @Override
    public void visit(MultiLineString multiLineString) {
        visit((GeometryCollection) multiLineString);
    }

    @Override
    public void visit(MultiPoint multiPoint) {
        visit((GeometryCollection) multiPoint);
    }

    @Override
    public void visit(MultiPolygon multiPolygon) {
        visit((GeometryCollection) multiPolygon);
    }

    @Override
    public void visit(LinearRing geom) {
        writeByteOrder(output);
        DimensionalFlag dimension = DimensionalFlag.parse(geom.is3D(), geom.isMeasured());
        writeTypeCodeAndSRID(geom, dimension, output);
        writeRing(geom);
    }

    private void writeRing(LinearRing geom) {
        output.putUInt(geom.getNumPoints());
        writePoints(geom.getPoints(), geom.getCoordinateDimension(), output);
    }

    private void writeNumRings(Polygon geom, ByteBuffer byteBuffer) {
        byteBuffer.putUInt(geom.isEmpty() ? 0 : geom.getNumInteriorRing() + 1);
    }

    protected void writePoint(double[] coordinates, ByteBuffer output) {
        for (double coordinate : coordinates) {
            output.putDouble(coordinate);
        }
    }

    protected void writePoints(PointSequence points, int coordinateDimension, ByteBuffer output) {
        double[] coordinates = new double[coordinateDimension];
        for (int i = 0; i < points.size(); i++) {
            points.getCoordinates(coordinates, i);
            writePoint(coordinates, output);
        }
    }

    protected void writeByteOrder(ByteBuffer output) {
        output.put(output.getWKBByteOrder().byteValue());
    }

    protected void writeTypeCodeAndSRID(Geometry geometry, DimensionalFlag dimension, ByteBuffer output) {
        int typeCode = getGeometryType(geometry);
        boolean hasSRID = (geometry.getSRID() > 0);
        if (hasSRID && !hasWrittenSRID)
            typeCode |= PostgisWkbTypeMasks.SRID_FLAG;
        if (dimension.isMeasured())
            typeCode |= PostgisWkbTypeMasks.M_FLAG;
        if (dimension.is3D())
            typeCode |= PostgisWkbTypeMasks.Z_FLAG;
        output.putUInt(typeCode);
        if (hasSRID && !hasWrittenSRID) {
            output.putInt(geometry.getSRID());
            hasWrittenSRID = true;
        }
    }

    protected int getGeometryType(Geometry geometry) {
        WkbGeometryType type = WkbGeometryType.forClass(geometry.getClass());
        if (type == null)
            throw new UnsupportedConversionException(String.format("Can't convert geometries of type %s", geometry.getClass().getCanonicalName()));
        return type.getTypeCode();
    }

}
}


