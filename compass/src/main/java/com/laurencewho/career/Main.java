package com.laurencewho.career;
import android.app.Activity;import android.content.*;import android.content.res.Configuration;import android.graphics.Typeface;import android.net.Uri;import android.os.*;import android.text.*;import android.view.*;import android.webkit.*;import android.widget.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;
/** compass (formerly who career) — generic, data-free. Native open calls / outreach / profile; fee book HTML in an isolated WebView. Everything personal stays on the device. */
public class Main extends Activity{
 static final String RAW="https://raw.githubusercontent.com/lollo259/who/main/data/";
 int fg,bg,dim;Typeface hv;float dp;SharedPreferences sp;FrameLayout body;TextView[] tabs=new TextView[4];int cur;WebView fee;FrameLayout feeBox;View dock;boolean dockShown=true;
 void dock(boolean on){if(on==dockShown)return;dockShown=on;dock.animate().translationY(on?0:dock.getHeight()+px(40)).alpha(on?1f:0f).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
 void watch(View v){v.setOnScrollChangeListener((x,a,y,b,oy)->{int d=y-oy;if(y<=0)dock(true);else if(d>6)dock(false);else if(d<-6)dock(true);});}
 JSONArray calls,outreach;String callsUpd="",orUpd="";String region="all",kind="all",stf="all";boolean openOnly=true;
 @Override protected void onCreate(Bundle b){super.onCreate(b);
  boolean night=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  fg=night?0xffffffff:0xff111111;bg=night?0xff000000:0xfff3f3f1;dim=night?0x80ffffff:0x80111111;
  hv=Typeface.create("sans-serif",Typeface.NORMAL);dp=getResources().getDisplayMetrics().density;sp=getSharedPreferences("c",0);
  getWindow().getDecorView().setBackgroundColor(bg);
  FrameLayout root=new FrameLayout(this);root.setBackgroundColor(bg);
  LinearLayout nav=new LinearLayout(this);dock=nav;nav.setGravity(Gravity.CENTER);nav.setPadding(px(8),px(4),px(8),px(4));nav.setElevation(px(6));
  android.graphics.drawable.GradientDrawable gd=new android.graphics.drawable.GradientDrawable();gd.setCornerRadius(px(28));gd.setColor(bg==0xff000000?0xf01c1c1c:0xf0ffffff);gd.setStroke(1,(fg&0x00ffffff)|0x22000000);nav.setBackground(gd);
  String[] n={"fee book","open calls","outreach","profile"};
  for(int i=0;i<4;i++){final int k=i;TextView t=tv(n[i],12,fg);t.setLetterSpacing(.12f);t.setPadding(px(9),px(10),px(9),px(10));t.setOnClickListener(v->show(k));tabs[i]=t;if(i==0&&!sp.getBoolean("showFee",true))t.setVisibility(View.GONE);nav.addView(t);}
  body=new FrameLayout(this);root.addView(body,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout.LayoutParams dl=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dl.bottomMargin=px(16);root.addView(nav,dl);
  root.setOnApplyWindowInsetsListener((v,in)->{dl.bottomMargin=px(16)+in.getSystemWindowInsetBottom();nav.setLayoutParams(dl);return in;});
  setContentView(root);immersive();
  seed=cached("opencalls.json",true);merge();
  show(b!=null?b.getInt("p",sp.getInt("p",0)):sp.getInt("p",0));}
 @Override protected void onResume(){super.onResume();refresh();}
 int px(float v){return (int)(v*dp+.5f);}
 TextView tv(String s,float size,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c);t.setTypeface(hv);t.setLineSpacing(0,1.25f);return t;}
 void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
 @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)immersive();}
 @Override protected void onSaveInstanceState(Bundle o){super.onSaveInstanceState(o);o.putInt("p",cur);}
 @Override public void onBackPressed(){if(detailOf!=null){show(cur);return;}if(cur==0&&fee!=null&&fee.canGoBack())fee.goBack();else if(cur>0)show(cur-1);else super.onBackPressed();}
 void show(int i){if(i==0&&!sp.getBoolean("showFee",true))i=1;cur=i;detailOf=null;if(dock!=null)dock(true);sp.edit().putInt("p",i).apply();for(int j=0;j<4;j++)tabs[j].setAlpha(j==i?1f:.35f);body.removeAllViews();
  if(i==0)body.addView(feePage());else if(i==1)body.addView(callsPage());else if(i==2)body.addView(outreachPage());else body.addView(profilePage());}
 // ---------- data: network → cache → bundled asset (never empty)
 JSONArray cached(String n,boolean c){String s=null;try{InputStream in=getAssets().open("w/data/"+n);s=new String(readAll(in),"UTF-8");}catch(Exception e){}
  return parse(s,c);}
 JSONArray parse(String s,boolean c){try{JSONObject o=new JSONObject(s);if(c)callsUpd=o.optString("updated");else orUpd=o.optString("updated");return o.getJSONArray("items");}catch(Exception e){return new JSONArray();}}
 static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int r;while((r=in.read(b))>0)o.write(b,0,r);in.close();return o.toByteArray();}
 JSONArray seed=new JSONArray();boolean refreshing;
 /** live calls straight from public feeds (no server of ours); bundled opencalls.json is only an offline seed */
 void refresh(){if(refreshing)return;refreshing=true;new Thread(()->{try{List<Feeds.Item> l=Feeds.fetchAll();if(!l.isEmpty()){JSONArray a=new JSONArray();
   for(Feeds.Item i:l){JSONObject o=new JSONObject();o.put("id",i.id);o.put("title",i.title);o.put("link",i.link);o.put("org",i.source);o.put("country",i.source);o.put("deadline",i.deadline);o.put("description",i.desc);o.put("type","open call");o.put("fee","see call");o.put("disciplines",new JSONArray(i.disc));o.put("live",true);a.put(o);}
   JSONObject w=new JSONObject();w.put("updated",new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date()));w.put("items",a);sp.edit().putString("live",w.toString()).apply();
   runOnUiThread(()->{merge();if(!isFinishing()&&cur==1&&detailOf==null)show(cur);});}}catch(Throwable e){}finally{refreshing=false;}}).start();}
 void merge(){JSONArray out=new JSONArray();Set<String> seen=new HashSet<>();String upd="";
  try{JSONObject w=new JSONObject(sp.getString("live","{}"));upd=w.optString("updated");JSONArray a=w.optJSONArray("items");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&seen.add(Feeds.norm(o.optString("title"))))out.put(o);}}catch(Exception e){}
  for(int i=0;i<seed.length();i++){JSONObject o=seed.optJSONObject(i);if(o!=null&&seen.add(Feeds.norm(o.optString("title"))))out.put(o);}
  calls=out;callsUpd=upd.isEmpty()?"offline seed":"live "+upd;}
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
 // ---------- open calls
 String q1="",q2="";View detailOf;boolean savedOnly;
 JSONObject saved(){try{return new JSONObject(sp.getString("saved","{}"));}catch(Exception e){return new JSONObject();}}
 boolean isSaved(JSONObject o){return saved().has(o.optString("id"));}
 void toggleSave(JSONObject o){JSONObject s=saved();String id=o.optString("id");if(id.isEmpty())return;if(s.has(id))s.remove(id);else{try{JSONObject m=new JSONObject();m.put("title",o.optString("title"));m.put("deadline",o.optString("deadline"));s.put(id,m);}catch(Exception e){}askNotify();}sp.edit().putString("saved",s.toString()).apply();Reminder.schedule(this);}
 void askNotify(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)try{requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},9);}catch(Exception e){}}
 TextView star(JSONObject o,Runnable after){TextView t=tv(isSaved(o)?"★":"☆",18,fg);t.setAlpha(isSaved(o)?1f:.45f);t.setPadding(px(10),0,0,0);t.setOnClickListener(v->{toggleSave(o);after.run();});return t;}
 static String hayT(JSONObject o){return (o.optJSONArray("roles")+" "+o.optJSONArray("disciplines")+" "+o.optJSONArray("tags"));}
 static String hayD(JSONObject o){return o.optString("type")+" "+o.optString("note")+" "+o.optString("description");}
 static int nRoles(JSONObject o){JSONArray a=o.optJSONArray("roles");return a==null?0:a.length();}
 android.graphics.drawable.GradientDrawable round(int color,float r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setCornerRadius(px(r));g.setColor(color);return g;}
 int soft(int a){return (fg&0x00ffffff)|(a<<24);}
 /** soft search bar: light fill, fully rounded, no shadow; filter icon inside (optional) */
 LinearLayout searchBar(String hint,String val,java.util.function.Consumer<String> on,Runnable filter){LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(px(18),0,px(6),0);bar.setBackground(round(soft(bg==0xff000000?0x1a:0x0d),26));
  TextView ic=tv("⌕",16,fg);ic.setAlpha(.45f);bar.addView(ic);
  EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(dim);e.setTextColor(fg);e.setTextSize(14);e.setTypeface(hv);e.setSingleLine(true);e.setBackground(null);e.setPadding(px(10),px(12),px(6),px(12));e.setText(val);
  e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence a,int b,int c,int d){}public void onTextChanged(CharSequence a,int b,int c,int d){}public void afterTextChanged(Editable x){on.accept(x.toString());}});
  bar.addView(e,new LinearLayout.LayoutParams(0,-2,1));
  if(filter!=null){TextView f=tv("☰",15,fg);f.setGravity(Gravity.CENTER);f.setBackground(round(soft(0x12),20));f.setOnClickListener(v->filter.run());bar.addView(f,new LinearLayout.LayoutParams(px(40),px(40)));}
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=px(8);bar.setLayoutParams(lp);return bar;}
 TextView pill(String text,boolean urg){TextView p=tv(text,10,gentleText(urg));p.setLetterSpacing(.06f);p.setPadding(px(10),px(4),px(10),px(4));p.setBackground(gentle(urg,14));return p;}
 String when(String dl){if(dl.isEmpty())return "no deadline listed";long d=daysTo(dl);return dl+(d>=0&&d<1000?"  ·  "+(d==0?"today":d==1?"1 day left":d+" days left"):"");}
 List<String> myJobs(){List<String> j=new ArrayList<>();for(String x:sp.getString("jobs","").toLowerCase(Locale.ROOT).split("[,;/]+"))if(!x.trim().isEmpty())j.add(x.trim());return j;}
 static String hayOf(JSONObject o){return (o.optJSONArray("roles")+" "+o.optJSONArray("disciplines")+" "+o.optJSONArray("tags")+" "+o.optString("title")+" "+o.optString("type")+" "+o.optString("note")+" "+o.optString("description")).toLowerCase(Locale.ROOT);}
 boolean night(){return bg==0xff000000;}
 /** gentle controls: selected = soft grey fill, unselected = thin light outline. never solid black. */
 android.graphics.drawable.GradientDrawable gentle(boolean on,float r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setCornerRadius(px(r));
  if(on)g.setColor(night()?0xff2a2a2a:0xffececec);else{g.setColor(0);g.setStroke(Math.max(1,px(1)),night()?0xff333333:0xffdddddd);}return g;}
 int gentleText(boolean on){return night()?(on?0xffe6e6e6:0xffaaaaaa):(on?0xff333333:0xff666666);}
 TextView chipBtn(String t,boolean on,Runnable r){TextView c=tv(t,12,gentleText(on));c.setPadding(px(14),px(8),px(14),px(8));c.setBackground(gentle(on,18));c.setOnClickListener(v->r.run());return c;}
 LinearLayout flow(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 /** simple wrapping row of chips */
 void wrap(LinearLayout box,List<View> chips){box.removeAllViews();int max=getResources().getDisplayMetrics().widthPixels-px(96);LinearLayout row=null;int w=0;
  for(View c:chips){c.measure(0,0);int cw=c.getMeasuredWidth()+px(8);if(row==null||w+cw>max){row=new LinearLayout(this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=px(8);box.addView(row,lp);w=0;}
   LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(-2,-2);cl.rightMargin=px(8);row.addView(c,cl);w+=cw;}}
 TextView heading(String t){TextView h=tv(t,10,dim);h.setLetterSpacing(.18f);h.setPadding(0,px(20),0,0);return h;}
 void filterSheet(){android.app.Dialog d=new android.app.Dialog(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(px(24),px(14),px(24),px(32));
  l.setBackground(round(bg==0xff000000?0xff161616:0xffffffff,28));View h=new View(this);h.setBackground(round(soft(0x33),3));LinearLayout.LayoutParams hl=new LinearLayout.LayoutParams(px(36),px(5));hl.gravity=Gravity.CENTER_HORIZONTAL;hl.bottomMargin=px(10);l.addView(h,hl);
  l.addView(center("filters",13,fg));
  LinearLayout jobsBox=flow(),regBox=flow(),stBox=flow();Runnable[] redraw=new Runnable[1];
  l.addView(heading("MY JOBS"));l.addView(jobsBox);
  LinearLayout addRow=new LinearLayout(this);addRow.setGravity(Gravity.CENTER_VERTICAL);EditText add=field("add a job");addRow.addView(add,new LinearLayout.LayoutParams(0,-2,1));
  Runnable doAdd=()->{String x=add.getText().toString().trim().toLowerCase(Locale.ROOT);if(x.isEmpty())return;List<String> j=myJobs();for(String y:x.split("[,;/]+"))if(!y.trim().isEmpty()&&!j.contains(y.trim()))j.add(y.trim());sp.edit().putString("jobs",String.join(", ",j)).apply();add.setText("");redraw[0].run();};
  addRow.addView(chipBtn("add",false,doAdd));add.setOnEditorActionListener((v,a,e)->{doAdd.run();return true;});l.addView(addRow);
  l.addView(line());l.addView(heading("REGION"));l.addView(regBox);l.addView(line());l.addView(heading("STATUS"));l.addView(stBox);
  redraw[0]=()->{List<View> jc=new ArrayList<>();for(String j:myJobs())jc.add(chipBtn(j+"  ×",true,()->{List<String> k=myJobs();k.remove(j);sp.edit().putString("jobs",String.join(", ",k)).apply();redraw[0].run();}));
   if(jc.isEmpty()){TextView e=tv("no jobs yet · add one or fill your profile",12,dim);e.setPadding(0,px(8),0,0);jc.add(e);}wrap(jobsBox,jc);
   List<View> rc=new ArrayList<>();String[] rk={"all","italy","europe","world"},rl={"everywhere","italy","europe","world"};for(int i=0;i<4;i++){String k=rk[i];rc.add(chipBtn(rl[i],region.equals(k),()->{region=k;redraw[0].run();}));}wrap(regBox,rc);
   List<View> sc=new ArrayList<>();sc.add(chipBtn("still open",openOnly&&!savedOnly,()->{openOnly=true;savedOnly=false;redraw[0].run();}));sc.add(chipBtn("include past",!openOnly&&!savedOnly,()->{openOnly=false;savedOnly=false;redraw[0].run();}));sc.add(chipBtn("★ saved",savedOnly,()->{savedOnly=true;redraw[0].run();}));wrap(stBox,sc);};
  redraw[0].run();
  TextView done=chipBtn("done",true,()->d.dismiss());done.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,-2);dl.topMargin=px(24);l.addView(done,dl);
  ScrollView sv=new ScrollView(this);sv.addView(l);d.setContentView(sv);d.setOnDismissListener(x->{if(cur==1&&!isFinishing())show(1);});
  Window w=d.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}d.show();}
 View callsPage(){LinearLayout l=new LinearLayout(this);ScrollView s=scroller(l);pull(s);
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);TextView sub=center("",11,dim);
  l.addView(searchBar("search calls",q1,x->{q1=x;drawCalls(list,sub);},this::filterSheet));l.addView(sub);
  l.addView(list);TextView f=center("from public feeds · "+callsUpd+" · pull down to refresh",10,dim);f.setPadding(0,px(24),0,0);l.addView(f);drawCalls(list,sub);return s;}
 void drawCalls(LinearLayout list,TextView sub){list.removeAllViews();List<String> jobs=myJobs();String q=q1.toLowerCase(Locale.ROOT).trim();
  String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());List<JSONObject> L=new ArrayList<>();
  for(int i=0;i<calls.length();i++){JSONObject o=calls.optJSONObject(i);if(o==null)continue;String dl=o.optString("deadline","");
   if(openOnly&&dl.length()==10&&dl.compareTo(today)<0)continue;
   if(!region.equals("all")&&!region.equals(o.optString("region")))continue;
   if(!q.isEmpty()&&!(o.optString("title")+" "+o.optString("org")+" "+o.optString("country")+" "+o.optString("note")+" "+o.optString("description")+" "+o.optString("type")).toLowerCase(Locale.ROOT).contains(q))continue;
   if(savedOnly&&!isSaved(o))continue;double sc=0;if(!jobs.isEmpty()){sc=Kw.spec(o.optString("title"),hayT(o),hayD(o),nRoles(o),jobs);if(sc==0&&!savedOnly)continue;}try{o.put("_s",sc);}catch(Exception e){}
   L.add(o);}
  L.sort((a,b)->{int c=Double.compare(b.optDouble("_s",0),a.optDouble("_s",0));return c!=0?c:a.optString("deadline","9").compareTo(b.optString("deadline","9"));});
  sub.setText(L.size()+(L.size()==1?" call":" calls")+(jobs.isEmpty()?"":" · best matches first")+(region.equals("all")?"":" · "+region)+(savedOnly?" · saved":""));sub.setPadding(0,px(2),0,px(4));
  for(JSONObject o:L){LinearLayout it=new LinearLayout(this);it.setOrientation(LinearLayout.VERTICAL);card(it);
   String dl=o.optString("deadline");long days=daysTo(dl);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(pill(when(dl),days>=0&&days<14));
   int st=Kw.strength(o.optDouble("_s",0));if(!jobs.isEmpty()){TextView sg=tv("●●●".substring(0,st)+"○○○".substring(st),9,fg);sg.setAlpha(.5f);sg.setLetterSpacing(.2f);sg.setPadding(px(10),0,0,0);top.addView(sg);}
   top.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));top.addView(star(o,()->drawCalls(list,sub)));
   LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-1,-2);plp.bottomMargin=px(10);it.addView(top,plp);
   it.addView(tv(o.optString("title"),15,fg));TextView m=tv(o.optString("country"),11,dim);m.setPadding(0,px(5),0,0);it.addView(m);
   List<String> hit=new ArrayList<>();String hay=hayOf(o);for(String j:jobs)if(Kw.score(hay,Collections.singletonList(j))>0)hit.add(j);
   if(!hit.isEmpty()){TextView ft=tv("fits: "+String.join(", ",hit),11,fg);ft.setAlpha(.85f);ft.setPadding(0,px(7),0,0);it.addView(ft);}
   it.setOnClickListener(v->callDetail(o,hit));list.addView(it);}
  if(L.isEmpty()){TextView e=center(savedOnly?"no saved calls yet — tap ☆ on a call":!q.isEmpty()?"no calls match your search":jobs.isEmpty()?"nothing open right now":"no calls for these jobs yet",13,dim);e.setPadding(0,px(40),0,0);list.addView(e);}}
 /** soft detail view inside the page; back returns to the list */
 void detail(LinearLayout c){ScrollView s=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(px(22),px(24),px(22),px(140));s.addView(l);watch(s);
  TextView back=tv("←  back",12,fg);back.setLetterSpacing(.1f);back.setAlpha(.6f);back.setPadding(px(4),0,0,px(12));back.setOnClickListener(v->show(cur));l.addView(back);
  card(c);l.addView(c);body.removeAllViews();body.addView(s);detailOf=s;if(dock!=null)dock(true);}
 TextView button(String t,Runnable r){TextView b=tv(t,13,gentleText(true));b.setGravity(Gravity.CENTER);b.setPadding(px(20),px(14),px(20),px(14));b.setBackground(gentle(true,26));b.setOnClickListener(v->r.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=px(18);b.setLayoutParams(lp);return b;}
 void callDetail(JSONObject o,List<String> hit){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);String dl=o.optString("deadline");long d=daysTo(dl);
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(pill(when(dl),d>=0&&d<14));top.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));top.addView(star(o,()->callDetail(o,hit)));c.addView(top,new LinearLayout.LayoutParams(-1,-2));TextView t=tv(o.optString("title"),20,fg);t.setPadding(0,px(12),0,0);c.addView(t);
  TextView m=tv(o.optString("org")+"\n"+o.optString("country")+"\n"+o.optString("type")+"  ·  fee "+o.optString("fee"),12,dim);m.setPadding(0,px(8),0,0);c.addView(m);
  String desc=(o.optString("description")+"\n"+o.optString("note")).trim();if(!desc.isEmpty()){TextView w=tv(desc,14,fg);w.setAlpha(.85f);w.setPadding(0,px(14),0,0);c.addView(w);}
  if(!hit.isEmpty()){TextView f=tv("fits: "+String.join(", ",hit),12,fg);f.setPadding(0,px(12),0,0);c.addView(f);}
  String link=o.optString("link");if(!link.isEmpty())c.addView(button("open the call",()->open(link)));detail(c);}
 static long daysTo(String d){try{Date t=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d);Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return Math.round((t.getTime()-c.getTimeInMillis())/86400000.0);}catch(Exception e){return -1;}}
 void card(LinearLayout it){it.setPadding(px(20),px(18),px(20),px(18));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setCornerRadius(px(24));
  g.setColor(bg==0xff000000?0xff151515:0xffffffff);if(bg==0xff000000)g.setStroke(1,0x14ffffff);it.setBackground(g);it.setElevation(bg==0xff000000?0:px(2));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(px(2),px(7),px(2),px(7));it.setLayoutParams(lp);}
 // ---------- outreach: the user's own list, stored on the device only
 static final String[] ST={"to contact","contacted","replied"};
 JSONArray contacts(){try{return new JSONArray(sp.getString("contacts","[]"));}catch(Exception e){return new JSONArray();}}
 EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(dim);e.setTextColor(fg);e.setTextSize(14);e.setTypeface(hv);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dim));return e;}
 TextView link(String s,Runnable r){TextView t=tv(s,11,fg);t.setLetterSpacing(.1f);t.setPadding(0,px(12),0,px(4));t.setOnClickListener(v->r.run());return t;}
 boolean adding;
 View outreachPage(){LinearLayout l=new LinearLayout(this);ScrollView s=scroller(l);JSONArray cs=contacts();String cv=sp.getString("cv","");
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
  l.addView(searchBar("search contacts",q2,x->{q2=x;drawContacts(list,cs,cv);},null));
  if(adding){EditText n=field("name"),k=field("kind — festival, gallery, label, person…"),pl=field("place"),ln=field("link — website, email or profile"),no=field("what they do (used to match your profile)");
   for(View v:new View[]{n,k,pl,ln,no})l.addView(v);no.setSingleLine(false);
   l.addView(button("save",()->{if(n.getText().toString().trim().isEmpty())return;try{JSONObject o=new JSONObject();o.put("id","c"+System.currentTimeMillis());o.put("name",n.getText().toString().trim());o.put("kind",k.getText().toString().trim());o.put("place",pl.getText().toString().trim());o.put("link",ln.getText().toString().trim());o.put("note",no.getText().toString().trim());cs.put(o);sp.edit().putString("contacts",cs.toString()).apply();}catch(Exception e){}adding=false;show(2);}));
  }else{TextView add=center("+ add a contact",13,fg);add.setPadding(0,px(8),0,px(4));add.setOnClickListener(v->{adding=true;show(2);});l.addView(add);}
  l.addView(list);drawContacts(list,cs,cv);
  TextView f=center("stored on this device only · nothing is sent from here",10,dim);f.setPadding(0,px(24),0,0);l.addView(f);return s;}
 void drawContacts(LinearLayout list,JSONArray cs,String cv){list.removeAllViews();String q=q2.toLowerCase(Locale.ROOT).trim();int n=0;
  for(int i=0;i<cs.length();i++){JSONObject o=cs.optJSONObject(i);if(o==null)continue;
   if(!q.isEmpty()&&!(o.optString("name")+" "+o.optString("kind")+" "+o.optString("place")+" "+o.optString("note")).toLowerCase(Locale.ROOT).contains(q))continue;n++;final int idx=i;
   String st=sp.getString("or_"+o.optString("id"),ST[0]);
   LinearLayout it=new LinearLayout(this);it.setOrientation(LinearLayout.VERTICAL);card(it);
   LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-2,-2);plp.bottomMargin=px(10);it.addView(pill(st,st.equals("replied")),plp);
   it.addView(tv(o.optString("name"),15,fg));TextView m=tv(o.optString("place"),11,dim);m.setPadding(0,px(5),0,0);it.addView(m);
   List<String> f=Kw.fits(cv,o.optString("kind")+" "+o.optString("note")+" "+o.optString("name"));
   TextView ft=tv(cv.isEmpty()?"fits: add your profile to see matches":f.isEmpty()?"fits: no shared terms yet":"fits: "+String.join(", ",f),11,fg);ft.setAlpha(f.isEmpty()?.4f:.85f);ft.setPadding(0,px(7),0,0);it.addView(ft);
   it.setOnClickListener(v->contactDetail(cs,idx,f));list.addView(it);}
  if(n==0){TextView e=center(cs.length()==0?"no contacts yet":"no contacts match your search",13,dim);e.setPadding(0,px(40),0,0);list.addView(e);}}
 void contactDetail(JSONArray cs,int idx,List<String> f){JSONObject o=cs.optJSONObject(idx);if(o==null)return;String id=o.optString("id");String st=sp.getString("or_"+id,ST[0]);
  LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);
  TextView p=pill(st+"  ·  tap to change",st.equals("replied"));p.setOnClickListener(v->{int k=Arrays.asList(ST).indexOf(st);sp.edit().putString("or_"+id,ST[(k+1)%3]).apply();contactDetail(cs,idx,f);});c.addView(p,new LinearLayout.LayoutParams(-2,-2));
  TextView t=tv(o.optString("name"),20,fg);t.setPadding(0,px(12),0,0);c.addView(t);TextView m=tv(o.optString("kind")+"  ·  "+o.optString("place"),12,dim);m.setPadding(0,px(8),0,0);c.addView(m);
  if(!o.optString("note").isEmpty()){TextView w=tv(o.optString("note"),14,fg);w.setAlpha(.85f);w.setPadding(0,px(14),0,0);c.addView(w);}
  if(!f.isEmpty()){TextView ft=tv("fits: "+String.join(", ",f),12,fg);ft.setPadding(0,px(12),0,0);c.addView(ft);}
  String lk=o.optString("link");if(!lk.isEmpty())c.addView(button("contact",()->open(lk.contains("@")&&!lk.startsWith("http")?"mailto:"+lk:lk.startsWith("http")?lk:"https://"+lk)));
  TextView del=center("delete contact",12,fg);del.setAlpha(.45f);del.setPadding(0,px(16),0,0);del.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("delete "+o.optString("name")+"?").setPositiveButton("delete",(d,w)->{cs.remove(idx);sp.edit().putString("contacts",cs.toString()).apply();show(2);}).setNegativeButton("cancel",null).show());c.addView(del);
  detail(c);}
 // ---------- profile / cv: on-device keyword matching, no network
 boolean editCv;
 void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/plain","application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});try{startActivityForResult(i,7);}catch(Exception e){Toast.makeText(this,"no file picker found",Toast.LENGTH_SHORT).show();}}
 LinearLayout pcard(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);card(c);return c;}
 TextView label(String t){TextView h=tv(t,10,dim);h.setLetterSpacing(.18f);h.setPadding(0,0,0,px(8));return h;}
 LinearLayout btnRow(String a,boolean aOn,Runnable ra,String b,Runnable rb){LinearLayout r=new LinearLayout(this);r.setPadding(0,px(12),0,0);
  TextView x=chipBtn(a,aOn,ra);x.setGravity(Gravity.CENTER);x.setPadding(0,px(11),0,px(11));TextView y=chipBtn(b,false,rb);y.setGravity(Gravity.CENTER);y.setPadding(0,px(11),0,px(11));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.rightMargin=px(8);r.addView(x,lp);r.addView(y,new LinearLayout.LayoutParams(0,-2,1));return r;}
 LinearLayout setRow(String t,View right){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(0,px(6),0,px(6));r.addView(tv(t,14,fg),new LinearLayout.LayoutParams(0,-2,1));r.addView(right);return r;}
 View profilePage(){LinearLayout l=new LinearLayout(this);ScrollView s=scroller(l);l.setPadding(px(18),px(24),px(18),px(130));
  String cvTxt=sp.getString("cv","");String name=sp.getString("name","").trim();List<String> jobs=myJobs();boolean empty=cvTxt.trim().isEmpty()&&jobs.isEmpty();
  // greeting
  LinearLayout g=pcard();TextView hi=tv("hi, "+(name.isEmpty()?"you":name),24,fg);hi.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));hi.setOnClickListener(v->askName());g.addView(hi);
  TextView sub=tv(empty?"compass by who · everything stays on this device. tap your name to change it.":"compass by who · your calls, contacts and fees, all on this device.",12,dim);sub.setPadding(0,px(4),0,0);g.addView(sub);
  if(!empty){int match=0,week=0;String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
   for(int i=0;i<calls.length();i++){JSONObject o=calls.optJSONObject(i);if(o==null)continue;String dl=o.optString("deadline","");if(dl.length()==10&&dl.compareTo(today)<0)continue;
    if(jobs.isEmpty()||Kw.spec(o.optString("title"),hayT(o),hayD(o),nRoles(o),jobs)>0){match++;long d=daysTo(dl);if(dl.length()==10&&d>=0&&d<=7)week++;}}
   TextView st=tv(match+(match==1?" call matches you":" calls match you")+"  ·  "+week+" close this week",13,fg);st.setPadding(0,px(12),0,0);st.setAlpha(.85f);g.addView(st);}
  l.addView(g);l.addView(sigCard());
  if(empty||editCv){LinearLayout e=pcard();e.setGravity(Gravity.CENTER_HORIZONTAL);
   TextView t=center(empty?"add your cv or type what you do":"your cv",17,fg);e.addView(t);
   TextView d=center("compass reads it here on your phone to find the calls that fit you. nothing is uploaded.",12,dim);d.setPadding(0,px(6),0,0);e.addView(d);
   EditText cv=new EditText(this);cv.setHint("e.g. pianist and composer, chamber music, film scores…");cv.setHintTextColor(dim);cv.setTextColor(fg);cv.setTextSize(13);cv.setTypeface(hv);cv.setGravity(Gravity.TOP);cv.setMinLines(5);cv.setMaxLines(14);cv.setText(cvTxt);
   cv.setPadding(px(14),px(12),px(14),px(12));cv.setBackground(gentle(false,18));LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(-1,-2);cl.topMargin=px(14);e.addView(cv,cl);
   e.addView(btnRow("save",true,()->{sp.edit().putString("cv",cv.getText().toString()).apply();applyCv();editCv=false;show(3);},"import file",this::pickFile));
   TextView f=center(".txt · .docx · .pdf",11,dim);f.setPadding(0,px(10),0,0);e.addView(f);l.addView(e);}
  if(!empty){
   LinearLayout jc=pcard();jc.addView(label("WHAT YOU DO"));LinearLayout box=flow();List<View> chips=new ArrayList<>();
   for(String j:jobs)chips.add(chipBtn(j+"  ×",true,()->{List<String> k=myJobs();k.remove(j);sp.edit().putString("jobs",String.join(", ",k)).apply();show(3);}));
   chips.add(chipBtn("+ add",false,this::askJob));wrap(box,chips);jc.addView(box);
   TextView jn=tv(cvTxt.isEmpty()?"typed by you · tap to remove":"found in your cv · tap to remove",12,dim);jn.setPadding(0,px(8),0,0);jc.addView(jn);l.addView(jc);
   JSONObject sv=saved();if(sv.length()>0){LinearLayout sc=pcard();sc.addView(label("SAVED"));int n=0;
    for(Iterator<String> it=sv.keys();it.hasNext()&&n<5;){String id=it.next();JSONObject o=sv.optJSONObject(id);if(o==null)continue;String dl=o.optString("deadline","");long d=daysTo(dl);
     LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(0,px(n==0?0:9),0,px(9));
     TextView tt=tv(o.optString("title"),13,fg);tt.setPadding(0,0,px(10),0);r.addView(tt,new LinearLayout.LayoutParams(0,-2,1));
     r.addView(pill(dl.length()==10?dl.substring(5)+" · "+(d<0?"closed":d+"d"):"no deadline",d>=0&&d<14&&dl.length()==10));
     r.setOnClickListener(v->{savedOnly=true;show(1);});if(n>0)sc.addView(line());sc.addView(r);n++;}
    if(n>0)l.addView(sc);}
   if(!editCv){LinearLayout cc=pcard();cc.addView(label("YOUR CV"));cc.addView(tv(cvTxt.isEmpty()?"no cv yet":String.format(Locale.US,"%,d characters · stored on this device",cvTxt.length()),12,dim));
    cc.addView(btnRow("edit cv",false,()->{editCv=true;show(3);},"import file",this::pickFile));l.addView(cc);}}
  // settings
  LinearLayout se=pcard();se.addView(label("SETTINGS"));boolean feeOn=sp.getBoolean("showFee",true),on=sp.getBoolean("remind",true);int days=sp.getInt("remindDays",7);
  se.addView(setRow("show fee calculator",chipBtn(feeOn?"on":"off",feeOn,()->{sp.edit().putBoolean("showFee",!feeOn).apply();if(tabs[0]!=null)tabs[0].setVisibility(!feeOn?View.VISIBLE:View.GONE);show(3);})));
  se.addView(setRow("remind me about saved calls",chipBtn(on?"on":"off",on,()->{sp.edit().putBoolean("remind",!on).apply();if(!on)askNotify();Reminder.schedule(this);show(3);})));
  LinearLayout dd=new LinearLayout(this);dd.setGravity(Gravity.CENTER_VERTICAL);dd.addView(chipBtn("−",false,()->{sp.edit().putInt("remindDays",Math.max(1,days-1)).apply();Reminder.schedule(this);show(3);}));
  TextView dv=tv(String.valueOf(days),15,fg);dv.setPadding(px(12),0,px(12),0);dd.addView(dv);dd.addView(chipBtn("+",false,()->{sp.edit().putInt("remindDays",Math.min(60,days+1)).apply();Reminder.schedule(this);show(3);}));
  LinearLayout dr=setRow("days before the deadline",dd);dr.setAlpha(on?1f:.4f);se.addView(dr);l.addView(se);
  String ver="";try{ver=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ex){}
  TextView vv=center("compass by who "+ver,10,dim);vv.setPadding(0,px(10),0,0);l.addView(vv);
  return s;}
 void askName(){EditText e=field("your name");e.setText(sp.getString("name",""));LinearLayout w=new LinearLayout(this);w.setPadding(px(22),px(8),px(22),0);w.addView(e,new LinearLayout.LayoutParams(-1,-2));
  new android.app.AlertDialog.Builder(this).setTitle("your name").setView(w).setPositiveButton("save",(d,x)->{sp.edit().putString("name",e.getText().toString().trim()).apply();show(3);}).setNegativeButton("cancel",null).show();}
 void askJob(){EditText e=field("e.g. pianist");LinearLayout w=new LinearLayout(this);w.setPadding(px(22),px(8),px(22),0);w.addView(e,new LinearLayout.LayoutParams(-1,-2));
  new android.app.AlertDialog.Builder(this).setTitle("add what you do").setView(w).setPositiveButton("add",(d,x)->{List<String> j=myJobs();for(String y:e.getText().toString().toLowerCase(Locale.ROOT).split("[,;/]+"))if(!y.trim().isEmpty()&&!j.contains(y.trim()))j.add(y.trim());sp.edit().putString("jobs",String.join(", ",j)).apply();show(3);}).setNegativeButton("cancel",null).show();}
 void applyCv(){LinkedHashMap<String,Integer> k=Kw.extract(sp.getString("cv",""));List<String> top=new ArrayList<>();for(String x:k.keySet()){if(top.size()<5)top.add(x);}sp.edit().putString("jobs",String.join(", ",top)).apply();}
 @Override protected void onActivityResult(int rq,int rs,Intent d){super.onActivityResult(rq,rs,d);if(rq!=7||rs!=RESULT_OK||d==null||d.getData()==null)return;
  try{Uri u=d.getData();String type=getContentResolver().getType(u);byte[] b=readAll(getContentResolver().openInputStream(u));String txt;
   if((type!=null&&type.contains("pdf"))||(b.length>4&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'))txt=Pdf.text(b);else if((type!=null&&type.contains("wordprocessing"))||(b.length>2&&b[0]=='P'&&b[1]=='K'))txt=docx(b);else txt=new String(b,"UTF-8");
   sp.edit().putString("cv",txt.trim()).apply();applyCv();editCv=false;show(3);}catch(Exception e){Toast.makeText(this,"could not read that file",Toast.LENGTH_SHORT).show();}}
 static String docx(byte[] b)throws IOException{java.util.zip.ZipInputStream z=new java.util.zip.ZipInputStream(new ByteArrayInputStream(b));java.util.zip.ZipEntry e;
  while((e=z.getNextEntry())!=null)if(e.getName().equals("word/document.xml")){String x=new String(readAll(z),"UTF-8");return android.text.Html.fromHtml(x.replaceAll("</w:p>","\n").replaceAll("<w:tab/>"," ").replaceAll("<[^>]+>",""),0).toString();}return "";}
 // ---------- fee book (unchanged HTML), isolated; recovers from renderer crashes instead of going blank
 View feePage(){if(feeBox!=null&&feeBox.getParent() instanceof ViewGroup){((ViewGroup)feeBox.getParent()).removeView(feeBox);}
  if(feeBox==null){feeBox=new FrameLayout(this);feeBox.setBackgroundColor(0xfff7f7f5);buildFee();}
  return feeBox;}
 void buildFee(){feeBox.removeAllViews();try{fee=new WebView(this);}catch(Throwable t){feeFallback();return;}
  fee.setBackgroundColor(0xfff7f7f5);WebSettings s=fee.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);
  fee.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(!"who.local".equals(u.getHost()))return null;
    try{return new WebResourceResponse("text/html","utf-8",getAssets().open("w/fee.html"));}catch(Exception e){return null;}}
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if("who.local".equals(r.getUrl().getHost()))return false;open(r.getUrl().toString());return true;}
   @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){feeBox.removeAllViews();try{v.destroy();}catch(Throwable t){}fee=null;feeFallback();return true;}});
  fee.addJavascriptInterface(new Bridge(),"C");
  watch(fee);feeBox.addView(fee,new FrameLayout.LayoutParams(-1,-1));fee.loadUrl("https://who.local/fee.html");}
 void feeFallback(){TextView t=center("fee book stopped\n\ntap to reopen",14,0xff1a1a1a);t.setOnClickListener(v->buildFee());feeBox.addView(t,new FrameLayout.LayoutParams(-1,-1));}

 // ---------- signature (profile) + agreement sharing (fee book)
 boolean sigEdit;
 View sigCard(){LinearLayout c=pcard();c.addView(label("YOUR SIGNATURE"));String saved=sp.getString("sig","");
  if(!saved.isEmpty()&&!sigEdit){ImageView iv=new ImageView(this);iv.setImageBitmap(Sig.png(saved,px(300),px(120),fg));iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.FIT_START);c.addView(iv,new LinearLayout.LayoutParams(-1,px(110)));
   TextView t=tv("saved "+sp.getString("sigDate","")+" · added to every agreement",11,dim);t.setPadding(0,px(6),0,0);c.addView(t);
   c.addView(btnRow("redo",false,()->{sigEdit=true;show(3);},"remove",()->{sp.edit().remove("sig").remove("sigDate").apply();new File(getFilesDir(),"sig.png").delete();show(3);}));return c;}
  TextView d=tv("sign once with your finger. it is added to your agreements and stays on this device.",12,dim);c.addView(d);
  FrameLayout box=new FrameLayout(this);android.graphics.drawable.GradientDrawable gd=new android.graphics.drawable.GradientDrawable();gd.setCornerRadius(px(18));gd.setColor(fg&0x0affffff|0x0a000000);gd.setStroke(px(1),dim&0x40ffffff,px(6),px(5));box.setBackground(gd);
  View base=new View(this);base.setBackgroundColor(dim&0x40ffffff);FrameLayout.LayoutParams bl=new FrameLayout.LayoutParams(-1,px(1),Gravity.BOTTOM);bl.setMargins(px(18),0,px(18),px(38));box.addView(base,bl);
  TextView hint=center("sign here",11,dim);box.addView(hint,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));hint.setPadding(0,0,0,px(12));
  Sig.Pad pad=new Sig.Pad(this,fg);pad.onChange=()->hint.setVisibility(pad.empty()?View.VISIBLE:View.GONE);box.addView(pad,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,px(170));pl.topMargin=px(14);c.addView(box,pl);
  c.addView(btnRow("save",true,()->{if(pad.empty()){Toast.makeText(this,"sign first",Toast.LENGTH_SHORT).show();return;}String e=pad.encode();
   String day=new java.text.SimpleDateFormat("d MMM yyyy",Locale.UK).format(new Date()).toLowerCase(Locale.ROOT);sp.edit().putString("sig",e).putString("sigDate",day).apply();
   try(FileOutputStream o=new FileOutputStream(new File(getFilesDir(),"sig.png"))){Sig.png(e,900,360,0xff1a1a1a).compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}catch(Exception x){}
   sigEdit=false;show(3);},"clear",pad::clear));
  return c;}
 String today(){return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
 void shareText(String subj,String txt){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,subj);i.putExtra(Intent.EXTRA_TEXT,txt);startActivity(Intent.createChooser(i,"send link"));}
 class Bridge{
  @JavascriptInterface public String sig(){return sp.getString("sig","");}
  @JavascriptInterface public void link(String text,String from,String client){runOnUiThread(()->{try{String u=Sig.link(text,from,client,sp.getString("sig",""),today());
   shareText("agreement to sign","please read and sign the agreement here:\n"+u);}catch(Exception e){Toast.makeText(Main.this,"could not make the link",Toast.LENGTH_SHORT).show();}});}
  @JavascriptInterface public void pdf(String text,String from,String client){runOnUiThread(()->{try{File f=Sig.pdf(Main.this,text.isEmpty()?" ":text,from,client,sp.getString("sig",""),today());
   Uri u=Uri.parse("content://com.laurencewho.career.files/"+Uri.encode(f.getName()));Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,u);i.setClipData(ClipData.newRawUri("",u));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   startActivity(Intent.createChooser(i,"save pdf"));}catch(Exception e){Toast.makeText(Main.this,"could not make the pdf",Toast.LENGTH_SHORT).show();}});}
 }
}
