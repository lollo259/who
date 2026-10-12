package com.laurencewho.career;
import android.content.*;import android.database.*;import android.graphics.*;import android.graphics.pdf.PdfDocument;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.text.*;import android.view.*;import org.json.*;import java.io.*;import java.security.MessageDigest;import java.util.*;import java.util.zip.Deflater;
/** Signature: finger pad, compact strokes (1000×400 grid, delta ints), link payload, native PDF. No libraries. */
public class Sig{
 static final int W=1000,H=400;static final String PAGE="https://laurencewho.com/who/sign/#";
 /** Finger pad. */
 public static class Pad extends View{
  final List<List<float[]>> st=new ArrayList<>();List<float[]> cur;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Runnable onChange;
  public Pad(Context c,int ink){super(c);p.setColor(ink);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(2.6f*c.getResources().getDisplayMetrics().density);}
  @Override public boolean onTouchEvent(MotionEvent e){getParent().requestDisallowInterceptTouchEvent(true);float x=e.getX()/getWidth()*W,y=e.getY()/getHeight()*H;
   switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:cur=new ArrayList<>();cur.add(new float[]{x,y});st.add(cur);break;
    case MotionEvent.ACTION_MOVE:if(cur!=null){for(int i=0;i<e.getHistorySize();i++)cur.add(new float[]{e.getHistoricalX(i)/getWidth()*W,e.getHistoricalY(i)/getHeight()*H});cur.add(new float[]{x,y});}break;
    case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:cur=null;getParent().requestDisallowInterceptTouchEvent(false);if(onChange!=null)onChange.run();break;}
   invalidate();return true;}
  @Override protected void onDraw(Canvas c){float sx=getWidth()/(float)W,sy=getHeight()/(float)H;for(List<float[]> s:st){Path pa=new Path();for(int i=0;i<s.size();i++){float[] q=s.get(i);if(i==0)pa.moveTo(q[0]*sx,q[1]*sy);else pa.lineTo(q[0]*sx,q[1]*sy);}if(s.size()==1)pa.rLineTo(.5f,0);c.drawPath(pa,p);}}
  void clear(){st.clear();invalidate();if(onChange!=null)onChange.run();}
  boolean empty(){return st.isEmpty();}
  /** simplified strokes as delta-encoded int arrays */
  String encode(){JSONArray out=new JSONArray();for(List<float[]> s:st){List<float[]> q=rdp(s,2.5f);JSONArray a=new JSONArray();int px=0,py=0;
    for(float[] f:q){int x=Math.round(Math.max(0,Math.min(W,f[0]))),y=Math.round(Math.max(0,Math.min(H,f[1])));a.put(x-px);a.put(y-py);px=x;py=y;}out.put(a);}return out.toString();}
 }
 static List<float[]> rdp(List<float[]> p,float eps){if(p.size()<3)return new ArrayList<>(p);int idx=0;float dm=0;float[] a=p.get(0),b=p.get(p.size()-1);
  for(int i=1;i<p.size()-1;i++){float d=segDist(p.get(i),a,b);if(d>dm){dm=d;idx=i;}}
  if(dm<=eps){List<float[]> r=new ArrayList<>();r.add(a);r.add(b);return r;}
  List<float[]> l=rdp(p.subList(0,idx+1),eps),r=rdp(p.subList(idx,p.size()),eps);l.remove(l.size()-1);l.addAll(r);return l;}
 static float segDist(float[] p,float[] a,float[] b){float dx=b[0]-a[0],dy=b[1]-a[1],L=dx*dx+dy*dy;if(L==0)return (float)Math.hypot(p[0]-a[0],p[1]-a[1]);
  float t=Math.max(0,Math.min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/L));return (float)Math.hypot(p[0]-a[0]-t*dx,p[1]-a[1]-t*dy);}
 /** absolute points from stored strokes JSON */
 static List<List<float[]>> abs(String json){List<List<float[]>> r=new ArrayList<>();try{JSONArray A=new JSONArray(json);for(int i=0;i<A.length();i++){JSONArray a=A.getJSONArray(i);List<float[]> s=new ArrayList<>();int x=0,y=0;
   for(int j=0;j+1<a.length();j+=2){x+=a.getInt(j);y+=a.getInt(j+1);s.add(new float[]{x,y});}r.add(s);}}catch(Exception e){}return r;}
 /** draw strokes fitted into a box, bottom-left aligned */
 static void draw(Canvas c,List<List<float[]>> S,float bx,float by,float bw,float bh,Paint p){float x0=1e9f,y0=1e9f,x1=-1e9f,y1=-1e9f;for(List<float[]> s:S)for(float[] q:s){x0=Math.min(x0,q[0]);y0=Math.min(y0,q[1]);x1=Math.max(x1,q[0]);y1=Math.max(y1,q[1]);}
  if(x1<x0)return;float k=Math.min(bw/Math.max(1,x1-x0),bh/Math.max(1,y1-y0));float oy=by+bh-(y1-y0)*k;
  for(List<float[]> s:S){Path pa=new Path();for(int i=0;i<s.size();i++){float[] q=s.get(i);float X=bx+(q[0]-x0)*k,Y=oy+(q[1]-y0)*k;if(i==0)pa.moveTo(X,Y);else pa.lineTo(X,Y);}if(s.size()==1)pa.rLineTo(.5f,0);c.drawPath(pa,p);}}
 static Bitmap png(String json,int w,int h,int ink){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Paint p=ink(ink,w/180f);draw(new Canvas(b),abs(json),w*.04f,h*.08f,w*.92f,h*.84f,p);return b;}
 static Paint ink(int c,float w){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(c);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(w);return p;}
 static String sha(String s){try{byte[] d=MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){return "";}}
 /** payload: {v,t,f,n,d,h,s} -> zlib deflate -> base64url */
 static String link(String text,String from,String client,String strokes,String date)throws Exception{JSONObject o=new JSONObject();o.put("v",1);o.put("t",text);o.put("f",from);o.put("n",client);o.put("d",date);o.put("h",sha(text+"\n"+date));o.put("s",new JSONArray(strokes.isEmpty()?"[]":strokes));
  byte[] in=o.toString().getBytes("UTF-8");Deflater df=new Deflater(9);df.setInput(in);df.finish();ByteArrayOutputStream bo=new ByteArrayOutputStream();byte[] buf=new byte[4096];while(!df.finished())bo.write(buf,0,df.deflate(buf));df.end();
  return PAGE+android.util.Base64.encodeToString(bo.toByteArray(),android.util.Base64.URL_SAFE|android.util.Base64.NO_PADDING|android.util.Base64.NO_WRAP);}
 /** A4 PDF with the agreement, his signature and an empty client line */
 static File pdf(Context cx,String text,String from,String client,String strokes,String date)throws Exception{PdfDocument doc=new PdfDocument();int PW=595,PH=842,M=56;
  TextPaint tp=new TextPaint(Paint.ANTI_ALIAS_FLAG);tp.setTextSize(10.5f);tp.setColor(0xff1a1a1a);tp.setTypeface(Typeface.SERIF);
  StaticLayout sl=StaticLayout.Builder.obtain(text,0,text.length(),tp,PW-2*M).setLineSpacing(3,1).build();int line=0,page=1;float y=0;PdfDocument.Page pg=null;Canvas c=null;
  while(line<sl.getLineCount()){pg=doc.startPage(new PdfDocument.PageInfo.Builder(PW,PH,page++).create());c=pg.getCanvas();int top=sl.getLineTop(line);int end=line;
   while(end<sl.getLineCount()&&sl.getLineBottom(end)-top<PH-2*M-140)end++;c.save();c.translate(M,M-top);c.clipRect(0,top,PW,sl.getLineBottom(end-1));sl.draw(c);c.restore();y=M+sl.getLineBottom(end-1)-top;line=end;if(line<sl.getLineCount())doc.finishPage(pg);}
  float cw=(PW-2*M-30)/2f;y+=24;draw(c,abs(strokes),M,y,cw,60,ink(0xff1a1a1a,1f));y+=66;Paint ln=new Paint();ln.setColor(0xffb4b4b4);ln.setStrokeWidth(.5f);c.drawLine(M,y,M+cw,y,ln);c.drawLine(M+cw+30,y,PW-M,y,ln);
  Paint sm=new Paint(Paint.ANTI_ALIAS_FLAG);sm.setTextSize(9);sm.setColor(0xff6e6e6e);c.drawText(from,M,y+13,sm);c.drawText(client.isEmpty()?"client":client,M+cw+30,y+13,sm);
  sm.setTextSize(7.5f);sm.setColor(0xff828282);c.drawText("agreement issued "+date+" · sha-256 of agreement text + issue date:",M,PH-M-6,sm);c.drawText(sha(text+"\n"+date),M,PH-M+6,sm);
  doc.finishPage(pg);File dir=new File(cx.getCacheDir(),"share");dir.mkdirs();for(File f:dir.listFiles())f.delete();
  String slug=client.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");File f=new File(dir,"agreement-"+(slug.isEmpty()?"client":slug)+"-"+date+".pdf");
  try(FileOutputStream o=new FileOutputStream(f)){doc.writeTo(o);}doc.close();return f;}
 /** Minimal read-only provider for files in cache/share (no androidx). */
 public static class Files extends ContentProvider{
  @Override public boolean onCreate(){return true;}
  File f(Uri u)throws FileNotFoundException{File d=new File(getContext().getCacheDir(),"share"),f=new File(d,u.getLastPathSegment());try{if(!f.getCanonicalPath().startsWith(d.getCanonicalPath()+"/"))throw new FileNotFoundException();}catch(IOException e){throw new FileNotFoundException();}return f;}
  @Override public ParcelFileDescriptor openFile(Uri u,String m)throws FileNotFoundException{return ParcelFileDescriptor.open(f(u),ParcelFileDescriptor.MODE_READ_ONLY);}
  @Override public String getType(Uri u){return "application/pdf";}
  @Override public Cursor query(Uri u,String[] pr,String s,String[] a,String o){MatrixCursor c=new MatrixCursor(new String[]{"_display_name","_size"});try{File f=f(u);c.addRow(new Object[]{f.getName(),f.length()});}catch(Exception e){}return c;}
  @Override public Uri insert(Uri u,ContentValues v){return null;}@Override public int delete(Uri u,String s,String[] a){return 0;}@Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}}
}
