/*
 * Copyright (C) 2017 GEOGRAT Informationssystem GmbH, Germany All rights reserved. This software is the confidential
 * and proprietary information of GEOGRAT Informationssystem GmbH (GEOGRAT). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the license agreement you entered into with
 * GEOGRAT. http://www.geograt.de
 */
package org.geolatte.geom;

import org.geolatte.geom.crs.CoordinateReferenceSystem;

/**
 * <p>
 * This is just a subclass of {@link Polygon} used within ORACLE spatial only.
 * </p>
 * <p>
 * In some special cases it is needed to find out if a polygon in ORACLE is defined as a rectangle.<br>
 * GEOGRAT GISX 2.x is using this behaviour to store geometries that are not compatible to ORACLE SDO specification. In
 * this case the entity in hibernate spatial needs to know if a polygon is such an envelope.<br>
 * Using funtion in polygon to check it is an envelope is not enough in this case, because there maybe geometries
 * consisting of envelopes.
 * </p>
 * 
 * @author Christian Marsch
 */
public class PolygonEnvelope<P extends Position> extends Polygon<P> {

	/**
	 * @param crs
	 *            see {@link Polygon#Polygon(CoordinateReferenceSystem)}
	 */
	public PolygonEnvelope(CoordinateReferenceSystem<P> crs) {
		super(crs);
	}

	/**
	 * @param positionSequence
	 *            see {@link Polygon#Polygon(PositionSequence, CoordinateReferenceSystem)}
	 * @param crs
	 *            see {@link Polygon#Polygon(PositionSequence, CoordinateReferenceSystem)}
	 */
	public PolygonEnvelope(PositionSequence<P> positionSequence, CoordinateReferenceSystem<P> crs) {
		super(positionSequence, crs);
	}

	/**
	 * @param rings
	 *            see {@link Polygon#Polygon(LinearRing...)}
	 */
	public PolygonEnvelope(LinearRing<P>... rings) {
		super(rings);
	}

}
