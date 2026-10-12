package com.laurencewho.career;
import java.io.*;import java.util.*;import java.util.zip.*;
/** Minimal on-device PDF text extraction: Flate/uncompressed content streams, Tj/TJ/'/" operators, literal + hex strings, ToUnicode CMaps (bfchar/bfrange). No layout; good enough for keyword matching. */
public class Pdf{
 public static String text(byte[] pdf){String raw=new String(pdf,java.nio.charset.StandardCharsets.ISO_8859_1);List<String> streams=new ArrayList<>();int i=0;
  while((i=raw.indexOf("stream",i))>=0){int dictStart=raw.lastIndexOf("<<",i);String dict=dictStart>=0?raw.substring(dictStart,i):"";int s=i+6;if(s<raw.length()&&raw.charAt(s)=='\r')s++;if(s<raw.length()&&raw.charAt(s)=='\n')s++;
   int e=raw.indexOf("endstream",s);if(e<0)break;byte[] b=Arrays.copyOfRange(pdf,s,e);if(dict.contains("FlateDecode"))b=inflate(b);else if(dict.contains("/Filter")){i=e+9;continue;}
   if(b!=null)streams.add(new String(b,java.nio.charset.StandardCharsets.ISO_8859_1));i=e+9;}
  Map<Integer,String> cmap=new HashMap<>();for(String s:streams)if(s.contains("beginbfchar")||s.contains("beginbfrange"))cmap(s,cmap);
  StringBuilder out=new StringBuilder();for(String s:streams)if(s.contains("BT"))ops(s,cmap,out);return out.toString().replaceAll("[ \\t]+"," ").trim();}
 static byte[] inflate(byte[] b){try{Inflater f=new Inflater();f.setInput(b);ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] buf=new byte[8192];while(!f.finished()){int n=f.inflate(buf);if(n==0&&(f.needsInput()||f.needsDictionary()))break;o.write(buf,0,n);}return o.toByteArray();}catch(Exception e){return null;}}
 static String hexU(String h){StringBuilder b=new StringBuilder();for(int i=0;i+4<=h.length();i+=4)b.append((char)Integer.parseInt(h.substring(i,i+4),16));return b.toString();}
 static void cmap(String s,Map<Integer,String> m){java.util.regex.Matcher c=java.util.regex.Pattern.compile("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>").matcher(s.replaceAll("(?s).*?beginbf","beginbf"));
  for(String blk:s.split("endbf")){boolean range=blk.contains("beginbfrange");int k=blk.indexOf(range?"beginbfrange":"beginbfchar");if(k<0)continue;String body=blk.substring(k);
   java.util.regex.Matcher r=java.util.regex.Pattern.compile(range?"<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>":"<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>").matcher(body);
   while(r.find())try{if(range){int a=Integer.parseInt(r.group(1),16),z=Integer.parseInt(r.group(2),16);int u=Integer.parseInt(r.group(3).length()>4?r.group(3).substring(0,4):r.group(3),16);for(int x=a;x<=z&&x-a<5000;x++)m.put(x,String.valueOf((char)(u+x-a)));}else m.put(Integer.parseInt(r.group(1),16),hexU(r.group(2)));}catch(Exception e){}}}
 static void ops(String s,Map<Integer,String> cm,StringBuilder out){int n=s.length();for(int i=0;i<n;i++){char c=s.charAt(i);
   if(c=='('){StringBuilder b=new StringBuilder();int depth=1;i++;for(;i<n&&depth>0;i++){char d=s.charAt(i);if(d=='\\'&&i+1<n){char x=s.charAt(++i);if(x=='n')b.append('\n');else if(x>='0'&&x<='7'){int v=x-'0';for(int k=0;k<2&&i+1<n&&s.charAt(i+1)>='0'&&s.charAt(i+1)<='7';k++)v=v*8+(s.charAt(++i)-'0');b.append((char)v);}else b.append(x);}else if(d=='(') {depth++;b.append(d);}else if(d==')'){depth--;if(depth>0)b.append(d);}else b.append(d);}i--;
    String t=b.toString();if(!cm.isEmpty()&&t.length()%2==0&&nonAscii(t)){StringBuilder u=new StringBuilder();for(int k=0;k+1<t.length();k+=2){String g=cm.get((t.charAt(k)<<8)|t.charAt(k+1));if(g!=null)u.append(g);}t=u.toString();}out.append(t);}
   else if(c=='<'&&i+1<n&&s.charAt(i+1)!='<'){int e=s.indexOf('>',i);if(e<0)break;String h=s.substring(i+1,e).replaceAll("\\s","");i=e;
    if(!cm.isEmpty()){int w=h.length()%4==0&&cm.keySet().stream().anyMatch(k->k>255)?4:2;for(int k=0;k+w<=h.length();k+=w)try{String g=cm.get(Integer.parseInt(h.substring(k,k+w),16));if(g!=null)out.append(g);}catch(Exception x){}}
    else for(int k=0;k+2<=h.length();k+=2)try{out.append((char)Integer.parseInt(h.substring(k,k+2),16));}catch(Exception x){}}
   else if(c=='T'&&i+1<n&&(s.charAt(i+1)=='*'||s.charAt(i+1)=='D'||s.charAt(i+1)=='m'))out.append(' ');
   else if(c=='E'&&i+1<n&&s.charAt(i+1)=='T')out.append('\n');
   else if(c==']'){}
   else if(c=='-'||Character.isDigit(c)){int j=i;while(j<n&&("0123456789.-".indexOf(s.charAt(j))>=0))j++;try{double v=Double.parseDouble(s.substring(i,j));if(v<-200)out.append(' ');}catch(Exception x){}i=j-1;}}}
 static boolean nonAscii(String t){for(int i=0;i<t.length();i++)if(t.charAt(i)<9||t.charAt(i)>126)return true;return false;}
 public static void main(String[] a)throws Exception{System.out.println(text(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0]))));}
}
