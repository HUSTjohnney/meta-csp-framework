package org.metacsp.utility.UI;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.metacsp.framework.ConstraintNetwork;
import org.metacsp.framework.Variable;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeDomain;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeVariable;
import org.metacsp.multi.spatioTemporal.paths.Pose;
import org.metacsp.multi.spatioTemporal.paths.TrajectoryEnvelope;
import org.metacsp.utility.logging.MetaCSPLogging;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.equations.Quad;
import de.erichseifert.vectorgraphics2d.Document;
import de.erichseifert.vectorgraphics2d.Processor;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import de.erichseifert.vectorgraphics2d.eps.EPSProcessor;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.pdf.PDFProcessor;
import de.erichseifert.vectorgraphics2d.svg.SVGProcessor;
import de.erichseifert.vectorgraphics2d.util.PageSize;

public class JTSDrawingPanel extends JPanel {

	private static final String[] COLOR_CHART = new String[]{
			"#000000", "#FFFF00", "#1CE6FF", "#FF34FF", "#FF4A46", "#008941", "#006FA6", "#A30059",
			"#FFDBE5", "#7A4900", "#0000A6", "#63FFAC", "#B79762", "#004D43", "#8FB0FF", "#997D87",
			"#5A0007", "#809693", "#FEFFE6", "#1B4400", "#4FC601", "#3B5DFF", "#4A3B53", "#FF2F80",
			"#61615A", "#BA0900", "#6B7900", "#00C2A0", "#FFAA92", "#FF90C9", "#B903AA", "#D16100",
			"#DDEFFF", "#000035", "#7B4F4B", "#A1C299", "#300018", "#0AA6D8", "#013349", "#00846F",
			"#372101", "#FFB500", "#C2FFED", "#A079BF", "#CC0744", "#C0B9B2", "#C2FF99", "#001E09",
			"#00489C", "#6F0062", "#0CBD66", "#EEC3FF", "#456D75", "#B77B68", "#7A87A1", "#788D66",
			"#885578", "#FAD09F", "#FF8A9A", "#D157A0", "#BEC459", "#456648", "#0086ED", "#886F4C",

			"#34362D", "#B4A8BD", "#00A6AA", "#452C2C", "#636375", "#A3C8C9", "#FF913F", "#938A81",
			"#575329", "#00FECF", "#B05B6F", "#8CD0FF", "#3B9700", "#04F757", "#C8A1A1", "#1E6E00",
			"#7900D7", "#A77500", "#6367A9", "#A05837", "#6B002C", "#772600", "#D790FF", "#9B9700",
			"#549E79", "#FFF69F", "#201625", "#72418F", "#BC23FF", "#99ADC0", "#3A2465", "#922329",
			"#5B4534", "#FDE8DC", "#404E55", "#0089A3", "#CB7E98", "#A4E804", "#324E72", "#6A3A4C",
			"#83AB58", "#001C1E", "#D1F7CE", "#004B28", "#C8D0F6", "#A3A489", "#806C66", "#222800",
			"#BF5650", "#E83000", "#66796D", "#DA007C", "#FF1A59", "#8ADBB4", "#1E0200", "#5B4E51",
			"#C895C5", "#320033", "#FF6832", "#66E1D3", "#CFCDAC", "#D0AC94", "#7ED379", "#012C58"
	};

	//	private static final String removedColor = "#F2F2F2";
	private static final String removedColor = "#D9D9D9";

	private static final long serialVersionUID = -2533567139276709334L;
	private static final int MARGIN = 5; 
	private HashMap<String,Geometry> geometries = new HashMap<String,Geometry>();
	private HashMap<String,Long> geometryAges = new HashMap<String,Long>(); 
	private HashMap<String,Boolean> emptyGeoms = new HashMap<String,Boolean>(); 
	private HashMap<String,Boolean> thickGeoms = new HashMap<String,Boolean>(); 
	private HashMap<String,Boolean> transpGeoms = new HashMap<String,Boolean>(); 
	private HashMap<String,Paint> polyColors = new HashMap<String,Paint>(); 
	private AffineTransform geomToScreen = null;
	private double scale = 1.0;
	private double userScale = 1.0;
	private AffineTransform panTrans = AffineTransform.getTranslateInstance(0.0, 0.0);
	private Logger metacsplogger = MetaCSPLogging.getLogger(this.getClass());
	private BufferedImage map = null;
	private double mapResolution = 1;
	private double mapX = 0.0;
	private double mapY = 0.0;
	private HashSet<String> permanentGeometries = new HashSet<String>();

	private boolean transformTouched = false;
	private TweenableEnvelope currentGeomBounds = null;
	private TweenableEnvelope oldGeomBounds = null;
	private TweenManager tweenManager = null;
	private long lastUpdateTime = Calendar.getInstance().getTimeInMillis();
	private boolean smoothTransitions = false;

	private VectorGraphics2D vg2d = null;
	private boolean dumpPDF = false;
	private String dumpFileName = null;
	private boolean dumpSVG = false;
	private boolean dumpEPS = false;

	private double targetTextSize = 1.0;
	private double targetArrowHeadWidth = 1.0;
	
	private double panAcceleration = 2.5;
	private double zoomIntensity = 0.05;

	public void setPanAcceleration(double accel) {
		this.panAcceleration = accel;
	}
	
	public void setZoomIntensity(double intens) {
		this.zoomIntensity = intens;
	}
	
	public void setSmoothTransitions(boolean value) {
		this.smoothTransitions = value;
	}
	
	public void setTextSizeInMeters(double targetTextSize) {
		this.targetTextSize = targetTextSize;
	}

	public void setArrowHeadSizeInMeters(double targetArrowHeadSize) {
		this.targetArrowHeadWidth = targetArrowHeadSize;
	}

	private void setCenteredPanTrans() {
		panTrans = AffineTransform.getTranslateInstance(0.0, 0.0);
	}

	public JTSDrawingPanel() {

		Tween.registerAccessor(TweenableEnvelope.class, new EnvelopeAccessor());
		Tween.setCombinedAttributesLimit(4);
		tweenManager = new TweenManager();
		
		this.setDoubleBuffered(true);

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (e.getClickCount() == 2) {
						transformTouched = false;
						resetVisualization();
					}
				}
				if (SwingUtilities.isMiddleMouseButton(e)) {
					try {
						AffineTransform geomToScreenInv = geomToScreen.createInverse();
						Point2D.Double clickedPoint = new Point2D.Double((double)e.getX(),(double)e.getY());
						Point2D.Double tClickedPoint = new Point2D.Double();
						geomToScreenInv.transform(clickedPoint, tClickedPoint);
						//metacsplogger.info("Clicked point (x,y) = (" + tClickedPoint.getX() + "," + tClickedPoint.getY() + ")");
					} catch (NoninvertibleTransformException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});

		this.addMouseMotionListener(new MouseAdapter() {
			int previousX;
			int previousY;

			@Override
			public void mousePressed(MouseEvent e) {
				previousX = e.getX();
				previousY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				transformTouched = true;
				int x = e.getX();
				int y = e.getY();
				if (SwingUtilities.isRightMouseButton(e)) {
					userScale += Math.signum(y-previousY)*zoomIntensity;
					if (userScale < 0.01) userScale = 0.01;		    		
				}
				else if (SwingUtilities.isLeftMouseButton(e)) {
//					double accel = 2.5;
//					if (map != null) {
//						accel *= 0.01*map.getHeight();
//					}
					panTrans = AffineTransform.getTranslateInstance(panTrans.getTranslateX()+Math.signum(x-previousX)*panAcceleration*userScale, panTrans.getTranslateY()-Math.signum(y-previousY)*panAcceleration*userScale);
				}
				previousX = x;
				previousY = y;
				updatePanel();
			}

		});

	}

	private Geometry createArrow(Pose pose1, Pose pose2) {		
		GeometryFactory gf = new GeometryFactory();
		double aux = 1.8;
		double distance = (1.6/targetArrowHeadWidth)*Math.sqrt(Math.pow((pose2.getX()-pose1.getX()),2)+Math.pow((pose2.getY()-pose1.getY()),2));
		double theta = Math.atan2(pose2.getY() - pose1.getY(), pose2.getX() - pose1.getX());
		Coordinate[] coords = new Coordinate[8];
		coords[0] = new Coordinate(0.0,-0.3);
		coords[1] = new Coordinate(distance-aux,-0.3);
		coords[2] = new Coordinate(distance-aux,-0.8);
		coords[3] = new Coordinate(distance,0.0);
		coords[4] = new Coordinate(distance-aux,0.8);
		coords[5] = new Coordinate(distance-aux,0.3);
		coords[6] = new Coordinate(0.0,0.3);
		coords[7] = new Coordinate(0.0,-0.3);
		Polygon arrow = gf.createPolygon(coords);
		AffineTransformation at = new AffineTransformation();
		at.scale(targetArrowHeadWidth/1.6, targetArrowHeadWidth/1.6);
		at.rotate(theta);
		at.translate(pose1.getX(), pose1.getY());
		Geometry ret = at.transform(arrow);
		return ret;
	}

	private Geometry createArrow(Pose pose) {
		GeometryFactory gf = new GeometryFactory();
		Coordinate[] coords = new Coordinate[8];
		coords[0] = new Coordinate(0.0,-0.3);
		coords[1] = new Coordinate(2.0,-0.3);
		coords[2] = new Coordinate(2.0,-0.8);
		coords[3] = new Coordinate(3.0,0.0);
		coords[4] = new Coordinate(2.0,0.8);
		coords[5] = new Coordinate(2.0,0.3);
		coords[6] = new Coordinate(0.0,0.3);
		coords[7] = new Coordinate(0.0,-0.3);
		Polygon arrow = gf.createPolygon(coords);
		AffineTransformation at = new AffineTransformation();
		at.scale(1.6/targetArrowHeadWidth, 1.6/targetArrowHeadWidth);
		at.rotate(pose.getTheta());
		at.translate(pose.getX(), pose.getY());
		Geometry ret = at.transform(arrow);
		return ret;
	}

	public synchronized void setMap(String mapYAMLFile) {
		try {
			File file = new File(mapYAMLFile);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String imageFileName = null;
			String st;
			while((st=br.readLine()) != null){
				if (!st.trim().startsWith("#") && !st.trim().isEmpty()) {
					String key = st.substring(0, st.indexOf(":")).trim();
					String value = st.substring(st.indexOf(":")+1).trim();
					if (key.equals("image")) imageFileName = file.getParentFile()+File.separator+value;
					else if (key.equals("resolution")) this.mapResolution = Double.parseDouble(value);
					else if (key.equals("origin")) {
						String x = value.substring(1, value.indexOf(",")).trim();
						String y = value.substring(value.indexOf(",")+1, value.indexOf(",", value.indexOf(",")+1)).trim();
						this.mapX = Double.parseDouble(x);
						this.mapY = Double.parseDouble(y);
					}
				}
			}
			br.close();
			this.map = ImageIO.read(new File(imageFileName));
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	public synchronized void addArrow(String arrowId, Pose pose) {
		if (arrowId != null) {
			arrowId = new String(arrowId);
			geometries.put(arrowId, createArrow(pose));
			geometryAges.put(arrowId, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(arrowId, false);
			transpGeoms.put(arrowId, false);
			thickGeoms.put(arrowId, false);
			polyColors.put(arrowId, Color.gray);
		}
	}

	public void setPermanent(String geometryID) {
		this.permanentGeometries.add(geometryID);
	}
	
	public boolean isPermanent(String geometryID) {
		return this.permanentGeometries.contains(geometryID);
	}
	
	public synchronized void addArrow(String arrowId, Pose pose, Color color) {
		if (arrowId != null) {
			arrowId = new String(arrowId);
			geometries.put(arrowId, createArrow(pose));
			geometryAges.put(arrowId, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(arrowId, false);
			transpGeoms.put(arrowId, false);
			thickGeoms.put(arrowId, false);
			polyColors.put(arrowId, color);
		}
	}

	public synchronized void addArrow(String arrowId, Pose pose1, Pose pose2) {
		if (arrowId != null) {
			arrowId = new String(arrowId);
			geometries.put(arrowId, createArrow(pose1, pose2));
			geometryAges.put(arrowId, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(arrowId, false);
			transpGeoms.put(arrowId, false);
			thickGeoms.put(arrowId, false);
			polyColors.put(arrowId, Color.gray);
		}
	}

	public synchronized void addArrow(String arrowId, Pose pose1, Pose pose2, Color color) {
		if (arrowId != null) {
			arrowId = new String(arrowId);
			geometries.put(arrowId, createArrow(pose1, pose2));
			geometryAges.put(arrowId, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(arrowId, false);
			transpGeoms.put(arrowId, false);
			thickGeoms.put(arrowId, false);
			polyColors.put(arrowId, color);
		}
	}

	public synchronized void resetVisualization() {
		userScale = 1.0;
		setCenteredPanTrans();
		updatePanel();
	}

	public synchronized void reinitVisualization() {
		userScale = 1.0;
		scale = 1.0;
		setCenteredPanTrans();
		geomToScreen = null;
	}

	public synchronized void addGeometry(String id, Geometry geom) {
		if (id != null) {
			id = new String(id);
			geometries.put(id,geom);
			geometryAges.put(id, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(id,false);
			thickGeoms.put(id,false);
			transpGeoms.put(id,true);
			Paint polyPaint = Color.decode(COLOR_CHART[(Math.abs(id.hashCode()))%COLOR_CHART.length]);
			polyColors.put(id,polyPaint);
		}
	}

	public synchronized void flushGeometries() {
		geometries.clear();
		emptyGeoms.clear();
		thickGeoms.clear();
		transpGeoms.clear();
		polyColors.clear();
	}

	public synchronized void addGeometry(String id, Geometry geom, boolean empty) {
		if (id != null) {
			id = new String(id);
			geometries.put(id,geom);
			geometryAges.put(id, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(id,empty);
			thickGeoms.put(id,false);
			transpGeoms.put(id,true);
			Paint polyPaint = Color.decode(COLOR_CHART[(Math.abs(id.hashCode()))%COLOR_CHART.length]);
			polyColors.put(id,polyPaint);
		}
	}

	public synchronized void addGeometry(String id, Geometry geom, boolean empty, boolean thick) {
		if (id != null) {
			id = new String(id);
			geometries.put(id,geom);
			geometryAges.put(id, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(id,empty);
			thickGeoms.put(id,thick);
			transpGeoms.put(id,true);
			Paint polyPaint = Color.decode(COLOR_CHART[(Math.abs(id.hashCode()))%COLOR_CHART.length]);
			polyColors.put(id,polyPaint);
		}
	}

	public synchronized void addGeometry(String id, Geometry geom, boolean empty, boolean thick, boolean transp) {
		if (id != null) {
			id = new String(id);
			geometries.put(id,geom);
			geometryAges.put(id, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(id,empty);
			thickGeoms.put(id,thick);
			transpGeoms.put(id,transp);
			Paint polyPaint = Color.decode(COLOR_CHART[(Math.abs(id.hashCode()))%COLOR_CHART.length]);
			polyColors.put(id,polyPaint);
		}
	}

	public synchronized void addGeometry(String id, Geometry geom, boolean empty, boolean thick, boolean transp, String color) { 
		if (id != null) {
			id = new String(id);
			geometries.put(id,geom);
			geometryAges.put(id, Calendar.getInstance().getTimeInMillis());
			emptyGeoms.put(id,empty);
			thickGeoms.put(id,thick);
			transpGeoms.put(id,transp);
			Paint polyPaint = Color.decode(color);
			polyColors.put(id,polyPaint);
		}
	}

	public synchronized void removeOldGeometries(long maxGeomAge) {
		for (Entry<String,Long> entry : geometryAges.entrySet()) {
			if (entry.getValue() > 0 && Calendar.getInstance().getTimeInMillis()-entry.getValue() > maxGeomAge) {
				//System.out.println("CLEANED UP VIZ OF " + entry.getKey());
				if (!isPermanent(entry.getKey())) removeGeometry(entry.getKey());
				//polyColors.put(entry.getKey(), Color.decode(removedColor));
			}
		}
	}

	public Coordinate getCoordinatesInRealWorld(Point clicked) {
		AffineTransform invTrans = new AffineTransform(geomToScreen);
		try { invTrans.invert(); }
		catch (NoninvertibleTransformException e) { e.printStackTrace(); }
		Point2D.Double transformed = new Point2D.Double(0, 0);
		invTrans.transform(new Point2D.Double(clicked.getX(), clicked.getY()), transformed);
		return new Coordinate(transformed.getX(), transformed.getY());
	}

	public synchronized void removeGeometry(String id) {
		if (id != null) {
			id = new String(id);
			geometries.remove(id); 
			emptyGeoms.remove(id);
			thickGeoms.remove(id);
			transpGeoms.remove(id);
			polyColors.remove(id);
			permanentGeometries.remove(id);
		}
	} 

	public void updatePanel() {
		this.repaint();
	}

	private AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return(AlphaComposite.getInstance(type, alpha));
	}

	private void drawText(Graphics2D g2d, String text, double x, double y, Paint polyPaint, boolean empty, boolean small) {
		g2d.setComposite(makeComposite(1.0f));
		if (vg2d != null) vg2d.setComposite(makeComposite(1.0f));

		AffineTransform textTrans = new AffineTransform();
		textTrans.setToScale(1.0,1.0);
		Font f = new Font("Sans", Font.BOLD, 8);
		if (small) f = new Font("Sans", Font.BOLD, 2);
		TextLayout tl = new TextLayout(text, f, g2d.getFontRenderContext());
		Shape shape = tl.getOutline(null);
		Shape newShape = textTrans.createTransformedShape(shape);
		double sizeOfText = Math.max(newShape.getBounds2D().getWidth(), newShape.getBounds2D().getHeight());
		double delta = -1;
		if (sizeOfText > targetTextSize) {
			delta = 0.99;
			while (sizeOfText > targetTextSize) {
				textTrans.scale(delta, delta);
				newShape = textTrans.createTransformedShape(newShape);
				sizeOfText = Math.max(newShape.getBounds2D().getWidth(), newShape.getBounds2D().getHeight());
			}
		}
		else {
			delta = 1.01;
			while (sizeOfText < targetTextSize) {
				textTrans.scale(delta, delta);
				newShape = textTrans.createTransformedShape(newShape);
				sizeOfText = Math.max(newShape.getBounds2D().getWidth(), newShape.getBounds2D().getHeight());
			}
		}

		AffineTransform newTrans = new AffineTransform(geomToScreen);
		newTrans.translate(x, y);
		newTrans.scale(1.0, -1.0);
		newShape = newTrans.createTransformedShape(newShape);

		g2d.setPaint(polyPaint);
		if (vg2d != null) vg2d.setPaint(polyPaint);
		if (!empty) {
			g2d.fill(newShape);
			if (vg2d != null) vg2d.fill(newShape);
		}
		else {
			g2d.draw(newShape);
			if (vg2d != null) vg2d.draw(newShape);
		}
	}
	
	public AffineTransform getMapTransform() {
//		AffineTransform mapTransform = (AffineTransform)geomToScreen.clone();
//		mapTransform.scale(this.mapResolution, -this.mapResolution);
//		mapTransform.translate(this.mapX, this.mapY-this.map.getHeight());	
		return this.geomToScreen;
	}

	@Override 
	protected synchronized void paintComponent(Graphics g) { 
		super.paintComponent(g); 

		if (smoothTransitions) {
			long currentUpdateTime = Calendar.getInstance().getTimeInMillis();
			tweenManager.update(currentUpdateTime-lastUpdateTime);
			lastUpdateTime = currentUpdateTime;
		}
		
		setTransform();
		if (dumpPDF || dumpSVG || dumpEPS) {
			vg2d = new VectorGraphics2D();
		}

		if(this.map != null) {
			Graphics2D g2d = (Graphics2D)g;
			AffineTransform mapTransform = (AffineTransform)geomToScreen.clone();
			mapTransform.scale(this.mapResolution, -this.mapResolution);
			mapTransform.translate(this.mapX, this.mapY-this.map.getHeight());			
			g2d.drawImage(this.map, mapTransform, this);
			//if (vg2d != null) vg2d.drawImage(this.map, mapTransform, this);
		}

		if (!geometries.isEmpty()) { 
			Graphics2D g2d = (Graphics2D) g; 
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			if (vg2d != null) {
				vg2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				vg2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
				vg2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				vg2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				vg2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}

			Paint defaultPaint = Color.BLACK; 
			Paint startCircPaint = Color.GREEN; 
			Paint endCircPaint = Color.RED; 

			for (Entry<String,Geometry> e : geometries.entrySet()) { 
				Geometry geom = e.getValue();
				boolean empty = emptyGeoms.get(e.getKey());
				boolean thick = thickGeoms.get(e.getKey());
				boolean transp = transpGeoms.get(e.getKey());
				ShapeWriter writer = new ShapeWriter();
				Shape shape = writer.toShape(geom);
				Shape newShape = geomToScreen.createTransformedShape(shape);
				if (geom instanceof Polygon) {
					Paint polyPaint = polyColors.get(e.getKey());
					if (transp) {
						g2d.setComposite(makeComposite(0.5f));
						if (vg2d != null) vg2d.setComposite(makeComposite(0.5f));
					}
					else {
						g2d.setComposite(makeComposite(1.0f));
						if (vg2d != null) vg2d.setComposite(makeComposite(1.0f));
					}
					g2d.setPaint(polyPaint);
					if (vg2d != null) vg2d.setPaint(polyPaint);

					if (thick) {
						g2d.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						if (vg2d != null) vg2d.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					}

					if (empty) {
						g2d.draw(newShape);
						if (vg2d != null) vg2d.draw(newShape);
					}
					else {
						g2d.fill(newShape);
						if (vg2d != null) vg2d.fill(newShape);
					}

					g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					if (vg2d != null) vg2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

					if (!e.getKey().startsWith("_")) {
						//Draw label
						String text = ""+e.getKey();
						if (text.startsWith("R")) drawText(g2d, text, geom.getCentroid().getX(), geom.getCentroid().getY(), Color.darkGray, empty, false);
						else drawText(g2d, text, geom.getCentroid().getX(), geom.getCentroid().getY(), Color.darkGray, empty, true);

					}
				}
				else {
					g2d.setPaint(defaultPaint);
					if (vg2d != null) vg2d.setPaint(defaultPaint);
					g2d.draw(newShape);
					if (vg2d != null) vg2d.draw(newShape);

					//draw start/end circles
					GeometricShapeFactory gsf = new GeometricShapeFactory();
					gsf.setSize(3);
					gsf.setCentre(geom.getCoordinates()[0]);
					Polygon startCirc = gsf.createCircle();
					gsf.setCentre(geom.getCoordinates()[geom.getCoordinates().length-1]);
					Polygon endCirc = gsf.createCircle();
					Shape startCircShape = writer.toShape(startCirc);
					Shape endCircShape = writer.toShape(endCirc);
					Shape newStartCircShape = geomToScreen.createTransformedShape(startCircShape);
					Shape newEndCircShape = geomToScreen.createTransformedShape(endCircShape);
					g2d.setStroke(new BasicStroke(2));
					g2d.setPaint(startCircPaint); 
					g2d.draw(newStartCircShape);
					g2d.setPaint(endCircPaint); 
					g2d.draw(newEndCircShape);
					if (vg2d != null) {
						vg2d.setStroke(new BasicStroke(2));
						vg2d.setPaint(startCircPaint); 
						vg2d.draw(newStartCircShape);
						vg2d.setPaint(endCircPaint); 
						vg2d.draw(newEndCircShape);						
					}
				} 
			}
			g2d.setComposite(makeComposite(1.0f));
			if (vg2d != null) vg2d.setComposite(makeComposite(1.0f));
		}
		if (dumpPDF || dumpSVG || dumpEPS) {
			CommandSequence commands = vg2d.getCommands();
			Processor processor = null;
			if (dumpPDF) processor = new PDFProcessor(false);
			else if (dumpSVG) processor = new SVGProcessor();
			else if (dumpEPS) processor = new EPSProcessor();
			//PageSize ps = new PageSize(this.getWidth(),this.getHeight());
			PageSize ps = new PageSize(getGeometryBounds().getWidth()*scale+2*MARGIN,getGeometryBounds().getHeight()*scale+2*MARGIN);
			Document doc = processor.getDocument(commands, ps);
			try { doc.writeTo(new FileOutputStream(dumpFileName)); }
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			dumpPDF = false;
			dumpSVG = false;
			dumpEPS = false;
			dumpFileName = null;
			vg2d.dispose();
			vg2d = null;
		}
	} 

	private void setTransform() {

		TweenableEnvelope env = getGeometryBounds();
		if (oldGeomBounds == null) {
			oldGeomBounds = env;
			currentGeomBounds = oldGeomBounds;
		}
		
		if (!transformTouched) {
			if (!env.equals(oldGeomBounds)) {
				if (smoothTransitions) {
					currentGeomBounds = oldGeomBounds;
					Tween.to(currentGeomBounds, 0, 500).target(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()).start(tweenManager);
				}
				else currentGeomBounds = env;
			}
			oldGeomBounds = env;
		}
		else currentGeomBounds = oldGeomBounds;

		Rectangle visRect = getVisibleRect();
		Rectangle drawingRect = new Rectangle(visRect.x + MARGIN, visRect.y + MARGIN, visRect.width - 2*MARGIN, visRect.height - 2*MARGIN); 
		scale = Math.min(drawingRect.getWidth() / currentGeomBounds.getWidth(), drawingRect.getHeight() / currentGeomBounds.getHeight()) * userScale;
		double xoff = MARGIN - scale * currentGeomBounds.getMinX();
		double yoff = MARGIN + scale * currentGeomBounds.getMaxY();
		double mapOffset = 0.0;
		//if (map != null) mapOffset = scale*map.getHeight();		
		geomToScreen = new AffineTransform(scale, 0, 0, -scale, xoff, yoff+0.5*mapOffset);
		geomToScreen.concatenate(panTrans);
	} 

	private TweenableEnvelope getGeometryBounds() { 
		Envelope env = new Envelope(); 
		for (Entry<String,Geometry> e : geometries.entrySet()) { 
			Geometry geom = e.getValue();
			Envelope geomEnv = geom.getEnvelopeInternal(); 
			env.expandToInclude(geomEnv); 
		}
		return new TweenableEnvelope(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()); 
	} 

	//	private Envelope getGeometryBounds() { 
	//		Envelope env = new Envelope(); 
	//		for (Entry<String,Geometry> e : geometries.entrySet()) { 
	//			Geometry geom = e.getValue();
	//			Envelope geomEnv = geom.getEnvelopeInternal(); 
	//			env.expandToInclude(geomEnv); 
	//		}
	//		return env; 
	//	} 

	public static void drawVariables(String title, boolean[] empty, GeometricShapeVariable[] vars) {
		JTSDrawingPanel panel = new JTSDrawingPanel(); 
		JFrame frame = new JFrame(title); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.add(panel); 
		frame.setSize(500, 500);
		for (int i = 0; i < vars.length; i++) {
			panel.emptyGeoms.put(vars[i].getID()+"", empty[i]);
			panel.addGeometry(vars[i].getID()+"",((GeometricShapeDomain)vars[i].getDomain()).getGeometry());
		}
		frame.setVisible(true);     	
	}

	public static void drawVariables(String title, GeometricShapeVariable ... vars) {
		JTSDrawingPanel panel = new JTSDrawingPanel(); 
		JFrame frame = new JFrame(title); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.add(panel); 
		frame.setSize(500, 500); 
		for (int i = 0; i < vars.length; i++) {
			panel.emptyGeoms.put(vars[i].getID()+"", false);
			panel.addGeometry(vars[i].getID()+"",((GeometricShapeDomain)vars[i].getDomain()).getGeometry());
		}
		frame.setVisible(true);     	
	}

	public static JTSDrawingPanel makeEmpty(String title) {
		JTSDrawingPanel panel = new JTSDrawingPanel(); 
		JFrame frame = new JFrame(title); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.add(panel); 
		frame.setSize(500, 500); 
		frame.setVisible(true);

		return panel;
	}


	public static void drawConstraintNetwork(String title, ConstraintNetwork cn) {
		ArrayList<GeometricShapeVariable> tes = new ArrayList<GeometricShapeVariable>();
		for (Variable v : cn.getVariables()) {
			if (v instanceof TrajectoryEnvelope) {
				TrajectoryEnvelope te = (TrajectoryEnvelope)v;
				if (!te.hasSubEnvelopes()) {
					tes.add(((TrajectoryEnvelope)v).getEnvelopeVariable());
					tes.add(((TrajectoryEnvelope)v).getReferencePathVariable());
				}
			}
		}
		drawVariables(title, tes.toArray(new GeometricShapeVariable[tes.size()]));
	}

	public void writePDF(String fileName) {
		dumpPDF = true;
		dumpFileName = fileName;
	}

	public void writeSVG(String fileName) {
		dumpSVG = true;
		dumpFileName = fileName;
	}

	public void writeEPS(String fileName) {
		dumpEPS = true;
		dumpFileName = fileName;
	}

} 
