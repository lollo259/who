// compass by who · signing page. Everything comes from the #fragment; nothing is sent anywhere.
"use strict";
const W=1000,H=400,$=id=>document.getElementById(id);
const b64u=s=>{s=s.replace(/-/g,"+").replace(/_/g,"/");while(s.length%4)s+="=";return Uint8Array.from(atob(s),c=>c.charCodeAt(0));};
async function inflate(bytes){const ds=new DecompressionStream("deflate");const r=new Blob([bytes]).stream().pipeThrough(ds);return new Uint8Array(await new Response(r).arrayBuffer());}
async function sha(s){const d=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(s));return [...new Uint8Array(d)].map(b=>b.toString(16).padStart(2,"0")).join("");}
// strokes: [[x0,y0,dx,dy,...],...] on a 1000×400 grid -> absolute point lists
const abs=S=>(S||[]).map(a=>{const p=[];let x=0,y=0;for(let i=0;i+1<a.length;i+=2){x+=a[i];y+=a[i+1];p.push([x,y]);}return p;});
function svg(P){if(!P.length)return "";let xs=P.flat().map(p=>p[0]),ys=P.flat().map(p=>p[1]);const x0=Math.min(...xs)-10,y0=Math.min(...ys)-10,w=Math.max(...xs)-x0+10,h=Math.max(...ys)-y0+10;
 return `<svg viewBox="${x0} ${y0} ${w} ${h}" preserveAspectRatio="xMinYMax meet"><path fill="none" stroke="currentColor" stroke-width="${Math.max(w,h)/110}" stroke-linecap="round" stroke-linejoin="round" d="${P.map(s=>"M"+s.map(p=>p.join(" ")).join("L")+(s.length==1?"l.1 0":"")).join("")}"/></svg>`;}
let D,HIS=[],CLI=[],cur=null,pdfBlob=null,fname="";
async function load(){
 const h=location.hash.slice(1);if(!h)throw new Error("empty");
 D=JSON.parse(new TextDecoder().decode(await inflate(b64u(h))));
 if(D.v!==1||typeof D.t!=="string")throw new Error("version");
 const real=await sha(D.t+"\n"+D.d);D.ok=real===D.h;
 HIS=abs(D.s);$("from").textContent=D.f||"—";$("text").textContent=D.t;$("hisSig").innerHTML=svg(HIS);$("hisCap").textContent=D.f||"";
 $("fp").innerHTML=(D.ok?"fingerprint ✓ ":"<span class=err>fingerprint does not match · do not sign</span> ")+"sha-256 "+real.replace(/(.{8})/g,"$1 ").trim()+" · issued "+D.d;
 if(D.n)$("name").value=D.n;$("view").classList.remove("hide");setupPad();check();
 if(!D.ok){$("sign").disabled=true;$("signCard").classList.add("hide");}
}
function check(){$("sign").disabled=!(D.ok&&CLI.length&&$("name").value.trim());$("cliCap").textContent=$("name").value.trim()||"you";}
function setupPad(){const cv=$("cv"),pad=$("pad"),cx=cv.getContext("2d");
 const fit=()=>{const r=pad.getBoundingClientRect(),k=devicePixelRatio||1;cv.width=r.width*k;cv.height=r.height*k;draw();};
 const draw=()=>{cx.setTransform(1,0,0,1,0,0);cx.clearRect(0,0,cv.width,cv.height);const sx=cv.width/W,sy=cv.height/H;cx.lineCap=cx.lineJoin="round";cx.lineWidth=2.4*(devicePixelRatio||1);cx.strokeStyle=getComputedStyle(document.body).color;
  for(const s of CLI){cx.beginPath();s.forEach((p,i)=>i?cx.lineTo(p[0]*sx,p[1]*sy):cx.moveTo(p[0]*sx,p[1]*sy));if(s.length==1)cx.lineTo(s[0][0]*sx+.5,s[0][1]*sy);cx.stroke();}};
 const pt=e=>{const r=cv.getBoundingClientRect();return [Math.round((e.clientX-r.left)/r.width*W),Math.round((e.clientY-r.top)/r.height*H)];};
 cv.onpointerdown=e=>{cv.setPointerCapture(e.pointerId);cur=[pt(e)];CLI.push(cur);$("hint").style.opacity=0;draw();};
 cv.onpointermove=e=>{if(!cur)return;const p=pt(e),q=cur[cur.length-1];if(Math.hypot(p[0]-q[0],p[1]-q[1])>=3){cur.push(p);draw();}};
 cv.onpointerup=cv.onpointercancel=()=>{cur=null;check();};
 $("clear").onclick=()=>{CLI=[];$("hint").style.opacity=1;draw();check();};$("name").oninput=check;
 addEventListener("resize",fit);fit();window.__draw=draw;}
async function font(doc){const b=new Uint8Array(await (await fetch("sans.ttf")).arrayBuffer());let s="";for(let i=0;i<b.length;i+=8192)s+=String.fromCharCode.apply(null,b.subarray(i,i+8192));
 doc.addFileToVFS("sans.ttf",btoa(s));doc.addFont("sans.ttf","sans","normal");doc.setFont("sans","normal");}
function sigPdf(doc,P,x,y,w,h){if(!P.length)return;const xs=P.flat().map(p=>p[0]),ys=P.flat().map(p=>p[1]);const x0=Math.min(...xs),y0=Math.min(...ys),sw=Math.max(...xs)-x0||1,sh=Math.max(...ys)-y0||1,k=Math.min(w/sw,h/sh);
 doc.setLineWidth(1);doc.setLineCap("round");doc.setLineJoin("round");for(const s of P){for(let i=1;i<s.length;i++)doc.line(x+(s[i-1][0]-x0)*k,y+h-sh*k+(s[i-1][1]-y0)*k,x+(s[i][0]-x0)*k,y+h-sh*k+(s[i][1]-y0)*k);}}
async function makePdf(name,when){const {jsPDF}=window.jspdf;const doc=new jsPDF({unit:"pt",format:"a4"});await font(doc);
 const M=56,PW=595.28,PH=841.89,LH=15;doc.setFontSize(10.5);doc.setTextColor(26);let y=M+10;
 for(const line of doc.splitTextToSize(D.t,PW-2*M)){if(y>PH-M-150){doc.addPage();y=M+10;}doc.text(line,M,y);y+=LH;}
 y+=24;if(y>PH-M-150){doc.addPage();y=M+10;}const cw=(PW-2*M-30)/2;
 sigPdf(doc,HIS,M,y,cw,60);sigPdf(doc,CLI,M+cw+30,y,cw,60);y+=66;doc.setDrawColor(180);doc.setLineWidth(.5);doc.line(M,y,M+cw,y);doc.line(M+cw+30,y,PW-M,y);
 doc.setFontSize(9);doc.setTextColor(110);doc.text(D.f||"",M,y+13);doc.text(name,M+cw+30,y+13);
 doc.setFontSize(7.5);doc.setTextColor(130);y=PH-M+6;
 doc.text("agreement issued "+D.d+" · signed by "+name+" on "+when,M,y-24);doc.text("sha-256 of agreement text + issue date: "+D.h,M,y-12);doc.text("compass by who · signed on the client's device, nothing stored online",M,y);
 return doc.output("blob");}
async function sign(){const name=$("name").value.trim();$("sign").disabled=true;$("sign").textContent="signing…";
 const now=new Date(),when=now.toLocaleString("en-GB",{dateStyle:"medium",timeStyle:"short"});
 pdfBlob=await makePdf(name,when);fname="agreement-"+(name.toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"")||"signed")+"-"+now.toISOString().slice(0,10)+".pdf";
 $("cliSig").innerHTML=svg(CLI);$("signCard").classList.add("hide");$("done").classList.remove("hide");
 $("doneLine").textContent="by "+name+" · "+when;$("share").innerHTML="send back to "+(D.f||"sender").toLowerCase()+"<small>whatsapp · email · signed pdf</small>";
 const f=new File([pdfBlob],fname,{type:"application/pdf"});
 if(!(navigator.canShare&&navigator.canShare({files:[f]}))){$("share").classList.add("hide");$("dl").classList.add("on");$("shareNote").textContent="download the pdf and send it back by email or whatsapp. nothing was uploaded.";}
 window.__pdfReady=true;$("done").scrollIntoView({behavior:"smooth"});}
$("sign").onclick=sign;
$("share").onclick=async()=>{const f=new File([pdfBlob],fname,{type:"application/pdf"});try{await navigator.share({files:[f],title:"signed agreement",text:"signed agreement"});}catch(e){}};
$("dl").onclick=()=>{const a=document.createElement("a");a.href=URL.createObjectURL(pdfBlob);a.download=fname;document.body.appendChild(a);a.click();a.remove();};
load().catch(e=>{$("bad").classList.remove("hide");window.__bad=String(e);});
