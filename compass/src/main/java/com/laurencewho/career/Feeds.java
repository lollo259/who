package com.laurencewho.career;
import java.io.*;import java.net.*;import java.util.*;import java.util.regex.*;
/** Live open calls straight from public RSS/Atom feeds. Pure Java, short timeouts, each source isolated. */
public class Feeds{
 public static final String[][] SOURCES={
  {"artjobs","https://www.artjobs.com/rss.xml"},
  {"alliance of artists communities","https://www.artistcommunities.org/rss.xml"},
  {"on the move","https://www.on-the-move.org/feed"},
  {"artribune","https://www.artribune.com/feed/","news"},
  {"exibart","https://www.exibart.com/feed/","news"},
  {"fondazione cariplo","https://www.fondazionecariplo.it/rss","news"}};
 static final Pattern CALL=Pattern.compile("(?i)open call|call for|calls for|residen|grant|fellowship|award|prize|premio|bando|bandi|concorso|competition|application|apply|deadline|scadenza|candidatur|opportunit|commission|submission|audition|casting");
 public static class Item{public String id,title,link,source,date,desc,deadline="";public List<String> disc=new ArrayList<>();}
 public static Map<String,Integer> counts=new LinkedHashMap<>();
 public static List<Item> fetchAll(){List<Item> all=new ArrayList<>();counts.clear();
  List<Thread> ts=new ArrayList<>();List<List<Item>> res=Collections.synchronizedList(new ArrayList<>());
  for(String[] s:SOURCES){Thread t=new Thread(()->{try{List<Item> l=parse(get(s[1]),s[0],s.length>2);res.add(l);synchronized(counts){counts.put(s[0],l.size());}}catch(Throwable e){synchronized(counts){counts.put(s[0],-1);}}});t.start();ts.add(t);}
  for(Thread t:ts)try{t.join(15000);}catch(InterruptedException e){}
  synchronized(res){for(List<Item> l:res)all.addAll(l);}
  // dedupe by link and normalized title
  Set<String> seen=new HashSet<>();List<Item> out=new ArrayList<>();for(Item i:all){String k=norm(i.title);if(seen.add(i.link)&&seen.add(k))out.add(i);}return out;}
 public static String norm(String t){return t.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
 static String get(String u)throws IOException{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Mozilla/5.0 compass");
  if(c.getResponseCode()!=200)throw new IOException("http "+c.getResponseCode());InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int r;while((r=in.read(b))>0&&o.size()<3_000_000)o.write(b,0,r);in.close();return o.toString("UTF-8");}
 static String tag(String block,String name){Matcher m=Pattern.compile("<"+name+"(?:\\s[^>]*)?>([\\s\\S]*?)</"+name+">").matcher(block);return m.find()?clean(m.group(1)):"";}
 static String clean(String s){s=s.replaceAll("<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>","$1");s=unesc(s);s=s.replaceAll("<[^>]+>"," ");s=unesc(s);return s.replaceAll("\\s+"," ").trim();}
 static String unesc(String s){s=s.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&#039;","'").replace("&#39;","'").replace("&apos;","'").replace("&nbsp;"," ").replace("&#8217;","’").replace("&#8211;","–").replace("&#8220;","“").replace("&#8221;","”");
  Matcher m=Pattern.compile("&#(\\d+);").matcher(s);StringBuffer b=new StringBuffer();while(m.find())m.appendReplacement(b,Matcher.quoteReplacement(String.valueOf((char)Integer.parseInt(m.group(1)))));m.appendTail(b);return b.toString().replace("&amp;","&");}
 public static List<Item> parse(String xml,String source,boolean news){List<Item> l=new ArrayList<>();boolean atom=!xml.contains("<item");
  Matcher m=Pattern.compile(atom?"<entry[\\s>][\\s\\S]*?</entry>":"<item[\\s>][\\s\\S]*?</item>").matcher(xml);
  while(m.find()){String b=m.group();Item i=new Item();i.source=source;i.title=tag(b,"title").replaceFirst("^\\[[^\\]]*\\]\\s*","");
   if(atom){Matcher h=Pattern.compile("<link[^>]*href=\"([^\"]+)\"").matcher(b);i.link=h.find()?h.group(1):"";}else i.link=tag(b,"link");
   i.date=tag(b,atom?"updated":"pubDate");String d=tag(b,"description");if(d.isEmpty())d=tag(b,atom?"summary":"content:encoded");if(d.isEmpty())d=tag(b,"content");i.desc=d.length()>600?d.substring(0,600)+"…":d;
   if(i.title.isEmpty()||i.link.isEmpty())continue;if(!CALL.matcher(news?i.title:i.title+" "+i.desc).find())continue;
   i.deadline=deadline(i.title+" "+d);long age=ageDays(i.date);String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
   if(!i.deadline.isEmpty()&&i.deadline.compareTo(today)<0)continue;if(i.deadline.isEmpty()&&age>90)continue;i.disc=classify(i.title+" "+d);i.id="f"+Integer.toHexString(i.link.hashCode());l.add(i);}
  return l;}
 /** days since pubDate/updated; 0 if unknown */
 static long ageDays(String d){for(String f:new String[]{"EEE, dd MMM yyyy HH:mm:ss Z","EEE, d MMM yyyy HH:mm:ss Z","yyyy-MM-dd'T'HH:mm:ssXXX","yyyy-MM-dd'T'HH:mm:ss.SSSXXX","EEE, dd MMM yyyy HH:mm:ss zzz"})try{return (System.currentTimeMillis()-new java.text.SimpleDateFormat(f,Locale.US).parse(d.trim()).getTime())/86400000L;}catch(Exception e){}return 0;}
 static final String[] MON={"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};
 static final String[] MES={"genn","febbr","marz","apri","magg","giugn","lugli","agost","sett","ottob","novem","dicem"};
 static int month(String w){w=w.toLowerCase(Locale.ROOT);for(int i=0;i<12;i++)if(w.startsWith(MON[i])||w.startsWith(MES[i]))return i+1;return 0;}
 /** finds a date near deadline words (EN/IT); returns yyyy-MM-dd or "" */
 public static String deadline(String t){Matcher k=Pattern.compile("(?i)(deadline|due|apply by|applications? (?:close|due)|closing date|until|entro (?:il)?|scadenza|scade|termine|fino al)").matcher(t);
  while(k.find()){String w=t.substring(k.end(),Math.min(t.length(),k.end()+60));String d=date(w);if(!d.isEmpty())return d;}return "";}
 static String date(String w){Matcher a=Pattern.compile("(\\d{1,2})(?:st|nd|rd|th|°)?\\s+([A-Za-zàé]{3,})\\.?,?\\s+(20\\d\\d)").matcher(w);if(a.find()&&month(a.group(2))>0)return fmt(a.group(3),month(a.group(2)),a.group(1));
  Matcher b=Pattern.compile("([A-Za-z]{3,})\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(20\\d\\d)").matcher(w);if(b.find()&&month(b.group(1))>0)return fmt(b.group(3),month(b.group(1)),b.group(2));
  Matcher c=Pattern.compile("(\\d{1,2})[./](\\d{1,2})[./](20\\d\\d)").matcher(w);if(c.find())return fmt(c.group(3),Integer.parseInt(c.group(2)),c.group(1));
  Matcher d=Pattern.compile("(20\\d\\d)-(\\d\\d)-(\\d\\d)").matcher(w);if(d.find())return d.group();return "";}
 static String fmt(String y,int m,String d){int dd=Integer.parseInt(d);if(m<1||m>12||dd<1||dd>31)return "";return String.format(Locale.US,"%s-%02d-%02d",y,m,dd);}
 static final String[][] DISC={{"music","music","musica","composer","compositore","sound","orchestra","concert","piano","song"},{"visual art","painting","pittura","sculpture","scultura","visual","arti visive","drawing","illustrat","photograph","fotografia"},
  {"performing arts","dance","danza","theatre","teatro","performance","circus","choreograph"},{"film","film","video","cinema","documentary","moving image"},{"literature","writing","writer","poetry","poesia","book","literary","letteratura"},
  {"design","design","architecture","architettura","craft"},{"multidisciplinary","multidisciplinary","interdisciplinary","all disciplines","any discipline","artists of all"},{"residency","residen"}};
 public static List<String> classify(String t){t=t.toLowerCase(Locale.ROOT);List<String> o=new ArrayList<>();for(String[] g:DISC)for(int i=1;i<g.length;i++)if(t.contains(g[i])){o.add(g[0]);break;}return o;}
 public static void main(String[] a){List<Item> l=fetchAll();System.out.println("per source: "+counts);System.out.println("total after dedupe: "+l.size());int dl=0;for(Item i:l)if(!i.deadline.isEmpty())dl++;System.out.println("with deadline: "+dl);
  for(Item i:l.subList(0,Math.min(12,l.size())))System.out.println("- ["+i.source+"] "+i.title+" | deadline="+i.deadline+" | "+i.disc);}
}
