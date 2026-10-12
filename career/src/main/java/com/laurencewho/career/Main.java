package com.laurencewho.career;
import android.app.Activity;import android.content.*;import android.content.res.Configuration;import android.graphics.Typeface;import android.net.Uri;import android.os.*;import android.text.*;import android.view.*;import android.webkit.*;import android.widget.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;
/** who career 0.3 — native pages (no WebView) for open calls and outreach; fee book = Laurence's own HTML in an isolated WebView that recovers if its renderer dies. */
public class Main extends Activity{
 static final String RAW="https://raw.githubusercontent.com/lollo259/who/main/data/";
 int fg,bg,dim;Typeface hv;float dp;SharedPreferences sp;FrameLayout body;TextView[] tabs=new TextView[3];int cur;WebView fee;FrameLayout feeBox;View dock;boolean dockShown=true;
 void dock(boolean on){if(on==dockShown)return;dockShown=on;dock.animate().translationY(on?0:dock.getHeight()+px(40)).alpha(on?1f:0f).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
 void watch(View v){v.setOnScrollChangeListener((x,a,y,b,oy)->{int d=y-oy;if(y<=0)dock(true);else if(d>6)dock(false);else if(d<-6)dock(true);});}
 JSONArray calls,outreach;String callsUpd="",orUpd="";String region="all",kind="all",stf="all";boolean openOnly=true;
 @Override protected void onCreate(Bundle b){super.onCreate(b);
  boolean night=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  fg=night?0xffffffff:0xff111111;bg=night?0xff000000:0xfffafafa;dim=night?0x80ffffff:0x80111111;
  hv=Typeface.create("sans-serif",Typeface.NORMAL);dp=getResources().getDisplayMetrics().density;sp=getSharedPreferences("c",0);
  getWindow().getDecorView().setBackgroundColor(bg);
  FrameLayout root=new FrameLayout(this);root.setBackgroundColor(bg);
  LinearLayout nav=new LinearLayout(this);dock=nav;nav.setGravity(Gravity.CENTER);nav.setPadding(px(8),px(4),px(8),px(4));nav.setElevation(px(6));
  android.graphics.drawable.GradientDrawable gd=new android.graphics.drawable.GradientDrawable();gd.setCornerRadius(px(28));gd.setColor(bg==0xff000000?0xf01c1c1c:0xf0ffffff);gd.setStroke(1,(fg&0x00ffffff)|0x22000000);nav.setBackground(gd);
  String[] n={"fee book","open calls","outreach"};
  for(int i=0;i<3;i++){final int k=i;TextView t=tv(n[i],12,fg);t.setLetterSpacing(.12f);t.setPadding(px(12),px(10),px(12),px(10));t.setOnClickListener(v->show(k));tabs[i]=t;nav.addView(t);}
  body=new FrameLayout(this);root.addView(body,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout.LayoutParams dl=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dl.bottomMargin=px(16);root.addView(nav,dl);
  root.setOnApplyWindowInsetsListener((v,in)->{dl.bottomMargin=px(16)+in.getSystemWindowInsetBottom();nav.setLayoutParams(dl);return in;});
  setContentView(root);immersive();
  calls=cached("opencalls.json",true);outreach=cached("outreach.json",false);
  show(b!=null?b.getInt("p",sp.getInt("p",0)):sp.getInt("p",0));}
 @Override protected void onResume(){super.onResume();refresh();}
 int px(float v){return (int)(v*dp+.5f);}
 TextView tv(String s,float size,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c);t.setTypeface(hv);t.setLineSpacing(0,1.25f);return t;}
 void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
 @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)immersive();}
 @Override protected void onSaveInstanceState(Bundle o){super.onSaveInstanceState(o);o.putInt("p",cur);}
 @Override public void onBackPressed(){if(cur==0&&fee!=null&&fee.canGoBack())fee.goBack();else if(cur>0)show(cur-1);else super.onBackPressed();}
 void show(int i){cur=i;if(dock!=null)dock(true);sp.edit().putInt("p",i).apply();for(int j=0;j<3;j++)tabs[j].setAlpha(j==i?1f:.35f);body.removeAllViews();
  if(i==0)body.addView(feePage());else if(i==1)body.addView(callsPage());else body.addView(outreachPage());}
 // ---------- data: network → cache → bundled asset (never empty)
 JSONArray cached(String n,boolean c){String s=sp.getString(n,null);if(s==null)try{InputStream in=getAssets().open("w/data/"+n);s=new String(readAll(in),"UTF-8");}catch(Exception e){}
  return parse(s,c);}
 JSONArray parse(String s,boolean c){try{JSONObject o=new JSONObject(s);if(c)callsUpd=o.optString("updated");else orUpd=o.optString("updated");return o.getJSONArray("items");}catch(Exception e){return new JSONArray();}}
 static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int r;while((r=in.read(b))>0)o.write(b,0,r);in.close();return o.toByteArray();}
 void refresh(){new Thread(()->{for(String n:new String[]{"opencalls.json","outreach.json"})try{HttpURLConnection c=(HttpURLConnection)new URL(RAW+n+"?t="+System.currentTimeMillis()).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setUseCaches(false);
   if(c.getResponseCode()==200){String s=new String(readAll(c.getInputStream()),"UTF-8");new JSONObject(s).getJSONArray("items");sp.edit().putString(n,s).apply();boolean isC=n.startsWith("open");
    runOnUiThread(()->{if(isC)calls=parse(s,true);else outreach=parse(s,false);if(!isFinishing()&&cur!=0)show(cur);});}}catch(Exception e){}}).start();}
 // ---------- shared views
 ScrollView scroller(LinearLayout l){ScrollView s=new ScrollView(this);s.setFillViewport(true);l.setOrientation(LinearLayout.VERTICAL);int side=px(26);l.setPadding(side,px(28),side,px(120));s.addView(l);watch(s);return s;}
 TextView center(String s,float size,int c){TextView t=tv(s,size,c);t.setGravity(Gravity.CENTER);return t;}
 HorizontalScrollView chips(String[] keys,String[] labels,String on,java.util.function.Consumer<String> pick){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER);
  for(int i=0;i<keys.length;i++){String k=keys[i];TextView t=tv(labels[i],12,fg);t.setPadding(px(10),px(6),px(10),px(6));t.setAlpha(k.equals(on)?1f:.38f);if(k.equals(on))t.setPaintFlags(t.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);t.setOnClickListener(v->pick.accept(k));r.addView(t);}
  HorizontalScrollView h=new HorizontalScrollView(this);h.setHorizontalScrollBarEnabled(false);h.addView(r);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.gravity=Gravity.CENTER_HORIZONTAL;h.setLayoutParams(lp);return h;}
 View line(){View v=new View(this);v.setBackgroundColor(fg);v.setAlpha(.12f);v.setLayoutParams(new LinearLayout.LayoutParams(-1,1));return v;}
 void open(String u){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){}}
 float y0=-1;
 void pull(ScrollView s){s.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN)y0=s.getScrollY()==0?e.getY():-1;
  else if(e.getAction()==MotionEvent.ACTION_UP){if(y0>=0&&e.getY()-y0>px(110)){Toast.makeText(this,"refreshing",Toast.LENGTH_SHORT).show();refresh();}y0=-1;}return false;});}
 static final String[][] SYN={
  {"pianist","pianista","piano","pianoforte","music","musica","musician","musicista","composer","compositore","composition","composizione","sound","suono"},
  {"musician","musicista","music","musica","sound","performer","composition"},
  {"artist","artista","art","arte","visual art","arti visive","interdisciplinary","multidisciplinary","multidisciplinare","interdisciplinare"},
  {"composer","compositore","composition","composizione","music","musica","score","film music","colonna sonora","film composer"},
  {"producer","produttore","music production","produzione","sound","creative producer","multidisciplinary"},
  {"filmmaker","regista","film","cinema","video","moving image","documentary","documentario"},
  {"designer","design","graphic","grafica","illustrat"},
  {"writer","scrittore","writing","scrittura","poet","poesia","literature","letteratura"},
  {"performer","performance","live","theatre","teatro","dance","danza"}};
 static Set<String> expand(String j){Set<String> o=new LinkedHashSet<>();o.add(j);for(String[] g:SYN)for(String w:g)if(w.equals(j)||(j.length()>3&&w.startsWith(j))){o.addAll(Arrays.asList(g));break;}return o;}
 // ---------- open calls
 View callsPage(){LinearLayout l=new LinearLayout(this);ScrollView s=scroller(l);pull(s);
  EditText jobs=new EditText(this);jobs.setText(sp.getString("jobs","pianist, composer, artist"));jobs.setHint("my jobs — pianist, composer, creative producer");jobs.setHintTextColor(dim);jobs.setTextColor(fg);jobs.setTextSize(14);jobs.setTypeface(hv);jobs.setGravity(Gravity.CENTER);jobs.setSingleLine(true);jobs.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dim));
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);TextView sub=center("",11,dim);
  Runnable draw=()->drawCalls(list,sub,jobs.getText().toString());
  jobs.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence a,int b,int c,int d){}public void onTextChanged(CharSequence a,int b,int c,int d){}public void afterTextChanged(Editable e){sp.edit().putString("jobs",e.toString()).apply();draw.run();}});
  l.addView(sub);l.addView(jobs);
  l.addView(chips(new String[]{"all","italy","europe","world"},new String[]{"everywhere","italy","europe","world"},region,k->{region=k;show(1);}));
  l.addView(chips(new String[]{"open","all"},new String[]{"still open","include past"},openOnly?"open":"all",k->{openOnly=k.equals("open");show(1);}));
  l.addView(list);TextView f=center("updated every monday · "+callsUpd,10,dim);f.setPadding(0,px(24),0,0);l.addView(f);draw.run();return s;}
 void drawCalls(LinearLayout list,TextView sub,String jobsTxt){list.removeAllViews();List<String> jobs=new ArrayList<>();for(String j:jobsTxt.toLowerCase().split("[,;/]+"))if(!j.trim().isEmpty())jobs.add(j.trim());
  String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());List<JSONObject> L=new ArrayList<>();
  for(int i=0;i<calls.length();i++){JSONObject o=calls.optJSONObject(i);if(o==null)continue;String dl=o.optString("deadline","");
   if(openOnly&&dl.length()==10&&dl.compareTo(today)<0)continue;
   if(!region.equals("all")&&!region.equals(o.optString("region")))continue;
   if(!jobs.isEmpty()){String hay=(o.optJSONArray("roles")+" "+o.optJSONArray("disciplines")+" "+o.optJSONArray("tags")+" "+o.optString("title")+" "+o.optString("type")+" "+o.optString("note")+" "+o.optString("description")).toLowerCase();boolean hit=false;for(String j:jobs)for(String w:expand(j))if(hay.contains(w))hit=true;if(!hit)continue;}
   L.add(o);}
  L.sort((a,b)->a.optString("deadline","9").compareTo(b.optString("deadline","9")));
  sub.setText(L.size()+(L.size()==1?" call":" calls"));
  for(JSONObject o:L){LinearLayout it=new LinearLayout(this);it.setOrientation(LinearLayout.VERTICAL);it.setPadding(0,px(16),0,px(16));
   it.addView(tv(o.optString("title"),15,fg));
   String dl=o.optString("deadline");long days=daysTo(dl);String when=dl+(days>=0&&days<1000?"  ·  "+(days==0?"today":days==1?"1 day left":days+" days left"):"");
   TextView m=tv(o.optString("org")+"  ·  "+o.optString("country")+"\n"+o.optString("type")+"  ·  fee "+o.optString("fee")+"\n"+when,11,dim);m.setPadding(0,px(5),0,0);it.addView(m);
   if(days>=0&&days<=14){m.setTextColor(fg);}
   String note=o.optString("note");if(!note.isEmpty()){TextView w=tv(note,12,fg);w.setAlpha(.75f);w.setPadding(0,px(7),0,0);it.addView(w);}
   TextView go=tv("open the call  →",11,fg);go.setLetterSpacing(.1f);go.setPadding(0,px(10),0,0);String link=o.optString("link");go.setOnClickListener(v->open(link));it.addView(go);
   list.addView(line());list.addView(it);}
  if(L.isEmpty()){TextView e=center(jobs.isEmpty()?"nothing open right now":"no calls for these jobs yet",13,dim);e.setPadding(0,px(40),0,0);list.addView(e);}}
 static long daysTo(String d){try{Date t=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d);Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return Math.round((t.getTime()-c.getTimeInMillis())/86400000.0);}catch(Exception e){return -1;}}
 // ---------- outreach
 static final String[] ST={"to contact","contacted","replied"};
 View outreachPage(){LinearLayout l=new LinearLayout(this);ScrollView s=scroller(l);TextView sub=center("",11,dim);l.addView(sub);
  l.addView(chips(new String[]{"all","festival","venue","gallery","label","person","company"},new String[]{"all","festivals","venues","galleries","labels","people","companies"},kind,k->{kind=k;show(2);}));
  l.addView(chips(new String[]{"all","to contact","contacted","replied"},new String[]{"any status","to contact","contacted","replied"},stf,k->{stf=k;show(2);}));
  int n=0;
  for(int i=0;i<outreach.length();i++){JSONObject o=outreach.optJSONObject(i);if(o==null)continue;String id=o.optString("id");String st=sp.getString("or_"+id,ST[0]);
   if(!kind.equals("all")&&!kind.equals(o.optString("kind")))continue;if(!stf.equals("all")&&!stf.equals(st))continue;n++;
   LinearLayout it=new LinearLayout(this);it.setOrientation(LinearLayout.VERTICAL);it.setPadding(0,px(16),0,px(16));
   it.addView(tv(o.optString("name"),15,fg));TextView m=tv(o.optString("kind")+"  ·  "+o.optString("place"),11,dim);m.setPadding(0,px(5),0,0);it.addView(m);
   TextView w=tv(o.optString("why"),12,fg);w.setAlpha(.75f);w.setPadding(0,px(7),0,0);it.addView(w);
   LinearLayout row=new LinearLayout(this);row.setPadding(0,px(10),0,0);row.setGravity(Gravity.CENTER_VERTICAL);
   TextView stv=tv(st,11,fg);stv.setLetterSpacing(.1f);stv.setAlpha(st.equals("replied")?1f:st.equals("contacted")?.7f:.4f);
   stv.setOnClickListener(v->{int k=Arrays.asList(ST).indexOf(st);sp.edit().putString("or_"+id,ST[(k+1)%3]).apply();show(2);});row.addView(stv,new LinearLayout.LayoutParams(0,-2,1));
   TextView go=tv("contact  →",11,fg);go.setLetterSpacing(.1f);String link=o.optString("link");go.setOnClickListener(v->open(link));row.addView(go);it.addView(row);
   l.addView(line());l.addView(it);}
  sub.setText(n+" · tap the status to change it");
  TextView f=center("nothing is sent from here · links open in your browser",10,dim);f.setPadding(0,px(24),0,0);l.addView(f);return s;}
 // ---------- fee book (unchanged HTML), isolated; recovers from renderer crashes instead of going blank
 View feePage(){if(feeBox!=null){((ViewGroup)feeBox.getParent()).removeView(feeBox);}
  if(feeBox==null){feeBox=new FrameLayout(this);feeBox.setBackgroundColor(0xfff7f7f5);buildFee();}
  return feeBox;}
 void buildFee(){feeBox.removeAllViews();try{fee=new WebView(this);}catch(Throwable t){feeFallback();return;}
  fee.setBackgroundColor(0xfff7f7f5);WebSettings s=fee.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);
  fee.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(!"who.local".equals(u.getHost()))return null;
    try{return new WebResourceResponse("text/html","utf-8",getAssets().open("w/fee.html"));}catch(Exception e){return null;}}
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if("who.local".equals(r.getUrl().getHost()))return false;open(r.getUrl().toString());return true;}
   @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){feeBox.removeAllViews();try{v.destroy();}catch(Throwable t){}fee=null;feeFallback();return true;}});
  watch(fee);feeBox.addView(fee,new FrameLayout.LayoutParams(-1,-1));fee.loadUrl("https://who.local/fee.html");}
 void feeFallback(){TextView t=center("fee book stopped\n\ntap to reopen",14,0xff1a1a1a);t.setOnClickListener(v->buildFee());feeBox.addView(t,new FrameLayout.LayoutParams(-1,-1));}
}
