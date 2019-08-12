package com.example.seamap;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static java.lang.Math.cos;
//
//class MyListener extends GestureDetector.SimpleOnGestureListener {
//    @Override
//    public boolean onDown(MotionEvent e) {
//        return true;
//    }
//
//}
class MapObject
{

    public  MapObject(String iname,int itype, float ilon,float ilat)
    {
        name = iname;
        Type=itype;
        lat=ilat;
        lon=ilon;
    }
    public String name;
    public int Type;
    public float lat,lon;
};
class MapPolyLine
{
    Map<Point,Vector<PointF>> cellList = new HashMap<Point,Vector<PointF>>();
}
public class MapView extends View {
    //        detector = new GestureDetector(this.getContext(), new MyListener());
    //    private List<Point> points = new ArrayList<>();
    Map<Point, Vector<MapObject>> cellObjectList =  new HashMap<Point,Vector<MapObject>>();
    Vector<MapPolyLine> plList = new Vector<MapPolyLine>();
    private int scrWidth ;
    private int scrHeight ;
    public void zoomin() {
        mScale*=1.5;
        invalidate();
    }
    public void zoomout() {
        mScale/=1.5;
        invalidate();
    }
    Point currentCell;
    private float mlat = 8.5f;//lattitude of the center of the screen
    private float mlon = 111.9f;//longtitude of the center of the screen
    private float mScale =3f;// 1km = mScale*pixcels
    private int scrCtY,scrCtX;
    Point pointtemp;
    PointF dragStart,dragStop;
    PointF pointTestlatlon;
    private Context mCtx;
    Button buttonZoomIn,buttonZoomOut;
    //private Point pt = new Point(scrWidth/2,scrHeight/2);
    GestureDetector detector;
    public MapView(Context context, AttributeSet attr) {
        super(context);
        mCtx =      context; //<-- fill it with the Context you are passed
        currentCell = new Point();
        LoadText();
//        buttonZoomIn = (Button) findViewById(R.id.button2);
//        buttonZoomIn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                zoomin();
//            }
//        });
//        buttonZoomOut = (Button) findViewById(R.id.button);
//        buttonZoomOut.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                zoomout();
//            }
//        });
    }

    @Override
    public void onDraw(Canvas canvas){// draw function
        scrCtY = getHeight()/2;
        scrCtX = getWidth()/2;
        canvas.drawColor(Color.rgb(0,0,0));
        Paint paint = new Paint();
        paint.setColor(Color.rgb(200,200,100));
        paint.setTextSize(14);
        PointF topRightLatLon = ConvScrPointToWGS(scrCtX*2,0);
        PointF botLeftLatLon = ConvScrPointToWGS(0,scrCtY*2);
        //draw test point
        pointtemp= ConvWGSToScrPoint(topRightLatLon.x,topRightLatLon.y);
        canvas.drawCircle(pointtemp.x, pointtemp.y, 20, paint);
        pointtemp= ConvWGSToScrPoint(botLeftLatLon.x,botLeftLatLon.y);
        canvas.drawCircle(pointtemp.x, pointtemp.y, 20, paint);
        //
        //scan all cells inside the screen and draw all text object
         for (int cellLon = (int)botLeftLatLon.x;cellLon<(int)topRightLatLon.x;cellLon+=1)
         {
             for (int cellLat = (int)botLeftLatLon.y;cellLat<(int)topRightLatLon.y;cellLat+=1)
             {
                 //draw data of current cell
                 currentCell = new Point(cellLon,cellLat);
                 if(cellObjectList.containsKey(currentCell)) {
                     Vector<MapObject> objectList = cellObjectList.get(currentCell);
                     for (MapObject obj : objectList) {
                         Point p = ConvWGSToScrPoint(obj.lon, obj.lat);
                         canvas.drawText(obj.name, p.x, p.y, paint);
                     }
                 }
             }
         }
         //draw polylines
        for (MapPolyLine pl:plList)
        {
            //for each line, loop over interested cells only
            for (int cellLon = (int)botLeftLatLon.x;cellLon<(int)topRightLatLon.x;cellLon+=1)
            {
                for (int cellLat = (int)botLeftLatLon.y;cellLat<(int)topRightLatLon.y;cellLat+=1) {
                    //draw data of current cell
                    currentCell = new Point(cellLon,cellLat);
                    Vector<PointF> pointfs = pl.cellList.get(currentCell);
                    if(pointfs==null)continue;
                    int size = pointfs.size()*2;
                    float pointis[] = new float[size];
                    for (int i=0;i<size;i+=2) {
                        Point p1 = ConvWGSToScrPoint(pointfs.elementAt(i/2).x, pointfs.elementAt(i/2).y);
                        pointis[i] = (float) p1.x;
                        pointis[i+1] =(float) p1.y;
                        if((int)(p1.x*p1.y)==0)
                        {
                            p1=p1;
                        }
                    }
                    canvas.drawLines(pointis,paint);
                }

            }
        }

    }
    private void LoadText()// load map from text file
    {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(mCtx.getAssets().open("lines.txt"), "UTF-8"));
            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                if(mLine.equals("Text"))
                {
                    String name =reader.readLine();
                    String latlon =reader.readLine();
                    String font =reader.readLine();
                    if(font.contains("(\"ABC Tahoma\",2,0,2105599)")) {
                        String[] strList= latlon.split(" ");
                        float lon = Float.parseFloat(strList[4]);
                        float lat = Float.parseFloat(strList[5]);
                        Point key = new Point((int)(lon),(int)(lat));
                        if(cellObjectList.containsKey(key)) {
                            Vector<MapObject> cell = (cellObjectList.get(key));
                            cell.add(new MapObject(name, 1, lon, lat));
                            cellObjectList.put(key,cell);
                        }
                        else
                        {
                            Vector<MapObject> newcell = new Vector<MapObject>();
                            newcell.add(new MapObject(name, 1, lon, lat));
                            cellObjectList.put(key,newcell);
                        }
                    }

                }
                else if(mLine.contains("Pline"))
                {

                    int numPoints = Integer.parseInt(mLine.split(" ")[1]);
                    MapPolyLine pl = new MapPolyLine();
                    for(int i=0;i<numPoints;i++)
                    {
                        String pointLatLon =reader.readLine();
                        String[] strList= pointLatLon.split(" ");
                        if(strList.length!=2)break;
                        float lon = Float.parseFloat(strList[0]);
                        float lat = Float.parseFloat(strList[1]);
                        Point key = new Point((int)lon,(int)lat);
                        if(pl.cellList.containsKey(key)) {
                            Vector<PointF> pointsInsideCell = (pl.cellList.get(key));
                            pointsInsideCell.add(new PointF( lon, lat));
                            pl.cellList.put(key,pointsInsideCell);
                        }
                        else
                        {
                            Vector<PointF> newcell = new Vector<PointF>();
                            newcell.add(new PointF(lon, lat));
                            pl.cellList.put(key,newcell);
                        }
                    }
                    plList.add(pl);
                }

            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
    }
    public Point ConvWGSToScrPoint(float m_Long,float m_Lat)// converting lat lon  WGS coordinates to screen XY coordinates
    {
        Point s = new Point();
        float refLat = (mlat + (m_Lat))*0.00872664625997f;//pi/360
        s.set((int )(mScale*((m_Long - mlon) * 111.31949079327357)*cos(refLat))+scrCtX,
                (int)(mScale*((mlat- (m_Lat)) * 111.132954))+getHeight()/2
        );
        return s;
    }
    public PointF ConvScrPointToWGS(int x,int y)
    {
        float olat  = mlat -  (float)(((y-scrCtY)/mScale)/(111.132954f));
        float refLat = (mlat +(olat))*0.00872664625997f;//3.14159265358979324/180.0/2;
        float olon = (x-scrCtX)/mScale/(111.31949079327357f*(float)cos(refLat))+ mlon;
        return new PointF(olon,olat);
    }
    //    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        points.add(new Point((int)event.getX(), (int)event.getY()));
//        invalidate();
//        return true;
//    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //boolean result = detector.onTouchEvent(event);
        boolean result;
//        if (!result) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //points.add(new Point((int)event.getX(), (int)event.getY()));
            //pt.set((int)event.getX(), (int)event.getY());
            dragStart = new PointF(event.getX(),event.getY());
            invalidate();
            result = true;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            //pt.set((int)event.getX(), (int)event.getY());
            //points.add(new Point((int)event.getX(), (int)event.getY()));
            dragStop = new PointF(event.getX(),event.getY());
            PointF newLatLon = ConvScrPointToWGS((int)(dragStart.x-dragStop.x)+scrCtX,(int)(dragStart.y-dragStop.y)+scrCtY);
            mlat=newLatLon.y;
            mlon=newLatLon.x;
            invalidate();
            result = true;
        }
        else if(event.getAction()==MotionEvent.ACTION_MOVE)
        {
            dragStop = new PointF(event.getX(),event.getY());
            PointF newLatLon = ConvScrPointToWGS((int)(dragStart.x-dragStop.x)+scrCtX,(int)(dragStart.y-dragStop.y)+scrCtY);
            mlat=newLatLon.y;
            mlon=newLatLon.x;
            invalidate();
            dragStart = dragStop;
            result = true;
        }

//        }
        return true;
    }

}
