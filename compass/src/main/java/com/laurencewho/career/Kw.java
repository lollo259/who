package com.laurencewho.career;
import java.util.*;
/** On-device keyword extraction: no AI, no network. Each group = a job + related terms (EN/IT). */
public class Kw{
 public static final String[][] G={
  {"pianist","pianista","piano","pianoforte","keyboard"},
  {"composer","compositore","composition","composizione","score","opera","chamber music","musica da camera","film music","colonna sonora"},
  {"musician","musicista","music","musica","concert","concerto","ensemble","trio","duo","orchestra","improvisation"},
  {"sound artist","sound","electroacoustic","elettroacustica","electronic","lo-fi","sound installation","audio"},
  {"producer","produttore","production","produzione","hip-hop","creative producer","beat"},
  {"artist","artista","art","arte","visual art","sculpture","scultura","installation","multidisciplinary","interdisciplinary","conceptual","multimedia"},
  {"performer","performance","live","stage","theatre","teatro","magic","illusion"},
  {"filmmaker","regista","film","short film","cortometraggio","video","moving image","cinema","documentary"},
  {"writer","scrittore","author","autore","book","libro","writing","articles","journalism","giornalismo","poetry"},
  {"designer","design","graphic","illustration","illustrazione"},
  {"architect","architetto","architecture","architettura"},
  {"teacher","insegnante","teaching","lessons","lezioni","education"}};
 /** returns group head → hit count, sorted by count desc */
 public static LinkedHashMap<String,Integer> extract(String text){String t=" "+text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\-]+"," ")+" ";
  Map<String,Integer> m=new HashMap<>();for(String[] g:G){int n=0;for(String w:g)n+=count(t," "+w);if(n>0)m.put(g[0],n);}
  List<Map.Entry<String,Integer>> l=new ArrayList<>(m.entrySet());l.sort((a,b)->b.getValue()-a.getValue());LinkedHashMap<String,Integer> o=new LinkedHashMap<>();for(Map.Entry<String,Integer> e:l)o.put(e.getKey(),e.getValue());return o;}
 static int count(String t,String w){int n=0,i=0;while((i=t.indexOf(w,i))>=0){n++;i+=w.length();}return n;}
 /** all terms of the groups of the given jobs (or free words) */
 public static Set<String> expand(String j){j=j.toLowerCase(Locale.ROOT).trim();Set<String> o=new LinkedHashSet<>();if(j.isEmpty())return o;o.add(j);for(String[] g:G)for(String w:g)if(w.equals(j)||(j.length()>3&&w.startsWith(j))){o.addAll(Arrays.asList(g));break;}return o;}
 /** score of a text against jobs */
 static boolean has(String hay,String w){int i=0;while((i=hay.indexOf(w,i))>=0){if(i==0||!Character.isLetter(hay.charAt(i-1)))return true;i++;}return false;}
 public static int score(String hay,List<String> jobs){hay=hay.toLowerCase(Locale.ROOT);int s=0;for(String j:jobs){if(has(hay,j))s+=3;else for(String w:expand(j))if(has(hay,w)){s+=1;break;}}return s;}
 /** CV terms (words of matched groups) that appear in both texts */
 public static List<String> fits(String cv,String contact){cv=cv.toLowerCase(Locale.ROOT);contact=contact.toLowerCase(Locale.ROOT);List<String> o=new ArrayList<>();for(String[] g:G)for(String w:g)if(has(cv,w)&&has(contact,w)&&!o.contains(w))o.add(w);return o;}

 /** generic terms count little; specific ones (piano, composition, electroacoustic…) a lot */
 static final Set<String> GENERIC=new HashSet<>(Arrays.asList("artist","artista","art","arte","musician","musicista","music","musica","creative","creative producer","performer","performance","live","stage","multidisciplinary","interdisciplinary","multidisciplinary artist","production","produzione","writing","education","video","design","sound","audio","production","culture","cultural"));
 static double w(String t){return GENERIC.contains(t)?1:4;}
 static double best(String field,String j){double b=0;for(String t:expand(j)){if(has(field,t)){double v=w(t)*(t.equals(j)?1:0.8);if(v>b)b=v;}}return b;}
 /** specificity score: title ×3, roles/disciplines/tags ×2, description/note/type ×1; divided down when a call lists very many roles */
 public static double spec(String title,String tags,String desc,int nRoles,List<String> jobs){title=title.toLowerCase(Locale.ROOT);tags=tags.toLowerCase(Locale.ROOT);desc=desc.toLowerCase(Locale.ROOT);double s=0;
  for(String j:jobs){double v=Math.max(3*best(title,j),Math.max(2*best(tags,j),best(desc,j)));s+=v;}
  return s/(1+Math.max(0,nRoles-6)/6.0);}
 /** 0..3 dots */
 public static int strength(double sc){return sc>=14?3:sc>=7?2:sc>0?1:0;}
 public static void main(String[] a)throws Exception{String cv=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])),"UTF-8");System.out.println(extract(cv));
  System.out.println("fits(festival w/ electroacoustic+installation): "+fits(cv,"experimental festival for electroacoustic music and sound installation"));
  System.out.println("score piano competition: "+score("international piano competition for pianists",new ArrayList<>(extract(cv).keySet())));}
}
