const WORKER_DEFAULT = "https://ps-ai-studio-seedance.ps-ai-studio.workers.dev";
const $ = (id) => document.getElementById(id);
let apiKey = "";
let workerUrl = WORKER_DEFAULT;
let mode = "text-to-video";
let lastJob = null;
let pollTimer = null;
let selectedImage = null;
const modelConfig = {
  seedance: { label:"Seedance 2.5", min:4, max:30, default:5, step:1, durationType:"string", route:"seedance" },
  "kling-v4": { label:"Kling 4.0", min:3, max:30, default:5, step:1, durationType:"number", route:"fal" },
  "wan-3": { label:"Wan 3.0", min:2, max:30, default:2, step:1, durationType:"number", route:"fal" },
  "hailuo-23": { label:"Hailuo 2.3 Pro", min:null, max:null, default:null, route:"fal" }
};
function setStatus(text, ok=false){ $("connection").textContent=(ok?"● ":"● ")+text; $("connection").style.color=ok?"#75d7bd":"#e6b477"; }
function setView(name){
  $("createView").classList.toggle("hidden",name!=="create");
  $("historyView").classList.toggle("hidden",name!=="history");
  document.querySelectorAll(".nav").forEach(b=>b.classList.toggle("active",b.dataset.tab===name));
  if(name==="history") renderHistory();
}
document.querySelectorAll(".nav").forEach(b=>b.addEventListener("click",()=>setView(b.dataset.tab)));
$("settingsBtn").addEventListener("click",()=>{$("apiKey").value="";$("workerUrl").value=workerUrl;$("settingsMessage").textContent=apiKey?"API key is loaded in this page memory. Enter a new key only if you want to replace it.":"No API key loaded yet."; $("settingsDialog").showModal();});
$("saveSettings").addEventListener("click",()=>{const k=$("apiKey").value.replace(/\s+/g,"");if(k)apiKey=k;workerUrl=$("workerUrl").value.trim().replace(/\/$/,"")||WORKER_DEFAULT;$("apiKey").value="";$("settingsMessage").textContent=apiKey?"Settings saved in this page session.":"Worker URL saved. Add your fal.ai key to generate.";setStatus(apiKey?"API key loaded":"API key required",!!apiKey);});
$("testConnection").addEventListener("click",async()=>{workerUrl=$("workerUrl").value.trim().replace(/\/$/,"")||WORKER_DEFAULT;try{const r=await fetch(workerUrl+"/health");$("settingsMessage").textContent=r.ok?"Worker reachable.":("Worker returned HTTP "+r.status+".");}catch(e){$("settingsMessage").textContent="Could not reach Worker: "+e.message;}});
$("settingsForm").addEventListener("submit",e=>e.preventDefault());
document.querySelectorAll(".mode").forEach(b=>b.addEventListener("click",()=>{mode=b.dataset.mode;document.querySelectorAll(".mode").forEach(x=>x.classList.toggle("active",x===b));$("imageField").classList.toggle("hidden",mode!=="image-to-video");}));
function updateDuration(){const cfg=modelConfig[$("model").value];const select=$("duration");select.innerHTML="";if(cfg.min===null){select.add(new Option("Model default","auto"));select.disabled=true;$("durationHint").textContent="Hailuo uses its model-defined duration.";}else{select.disabled=false;for(let n=cfg.min;n<=cfg.max;n+=cfg.step)select.add(new Option(n+" seconds",String(n)));select.value=String(cfg.default);$("durationHint").textContent="Supported range: "+cfg.min+"–"+cfg.max+" seconds.";}}
$("model").addEventListener("change",updateDuration);updateDuration();
$("prompt").addEventListener("input",()=>$("charCount").textContent=$("prompt").value.length+" / 20000");
$("image").addEventListener("change",async e=>{const file=e.target.files?.[0];selectedImage=null;if(!file)return;if(file.size>12*1024*1024){showError("Image must be 12 MB or smaller.");e.target.value="";return;}selectedImage=await new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(r.result);r.onerror=reject;r.readAsDataURL(file);});});
function showState(which){["emptyState","progressState","errorState","videoState"].forEach(id=>$(id).classList.toggle("hidden",id!==which));}
function showError(msg){showState("errorState");$("errorText").textContent=msg;$("generate").disabled=false;$("generate").textContent="✦ Generate video";}
function authHeaders(){if(!apiKey)throw new Error("Open Settings and enter your fal.ai API key first.");return {"Authorization":"Bearer "+apiKey.replace(/\s+/g,""),"Content-Type":"application/json"};}
function endpointFor(model,action,id){const cfg=modelConfig[model];const base=cfg.route==="seedance"?"/v1/seedance/"+(mode):"/v1/fal/"+model+"/"+mode;return workerUrl+base+(action?"/requests/"+encodeURIComponent(id)+"/"+action:"");}
async function readJson(response){const text=await response.text();let data;try{data=JSON.parse(text);}catch{throw new Error("Worker returned an invalid response (HTTP "+response.status+").");}if(!response.ok)throw new Error(data.error||data.message||("HTTP "+response.status));return data;}
async function generate(){const prompt=$("prompt").value.trim();if(!prompt){showError("Enter a prompt before generating.");return;}if(mode==="image-to-video"&&!selectedImage){showError("Choose a starting image first.");return;}if(!apiKey){$("settingsDialog").showModal();$("settingsMessage").textContent="Enter your fal.ai API key to continue.";return;}const model=$("model").value,cfg=modelConfig[model];const duration=$("duration").value;const body={prompt,aspect_ratio:$("ratio").value,resolution:$("resolution").value,audio:$("audio").checked};if(cfg.durationType==="string")body.duration=String(duration);else if(cfg.min!==null)body.duration=Number(duration);if(mode==="image-to-video")body.image_url=selectedImage;
  lastJob={model,mode,prompt,body};$("generate").disabled=true;$("generate").textContent="Submitting…";showState("progressState");$("progressTitle").textContent="Submitting request…";$("progressText").textContent="Sending your request to "+cfg.label+"."; $("requestId").textContent="";
  try{const response=await fetch(endpointFor(model,""),{method:"POST",headers:authHeaders(),body:JSON.stringify(body)});const data=await readJson(response);const id=data.request_id||data.requestId;if(!id)throw new Error("The model did not return a request ID: "+JSON.stringify(data).slice(0,500));$("requestId").textContent="Request ID: "+id;$("progressTitle").textContent="Video is generating";$("progressText").textContent="Request accepted. Waiting for the model to finish.";setStatus("Request submitted",true);poll(id,model);}
  catch(e){showError(e.message);setStatus("Request failed");}}
async function poll(id,model){if(pollTimer)clearTimeout(pollTimer);const cfg=modelConfig[model];try{const status=await readJson(await fetch(endpointFor(model,"status",id),{headers:authHeaders()}));const state=String(status.status||status.state||"").toLowerCase();$("progressText").textContent="Model status: "+(state||"processing")+".";
    if(["completed","complete","succeeded"].includes(state)){const result=await readJson(await fetch(endpointFor(model,"result",id),{headers:authHeaders()}));const videoUrl=result.video?.url||result.video_url||result.output?.video?.url||result.output?.video_url||result.data?.video?.url||result.url;if(!videoUrl)throw new Error("Generation completed, but no video URL was found in the result: "+JSON.stringify(result).slice(0,700));$("resultVideo").src=videoUrl;$("downloadVideo").href=videoUrl;showState("videoState");$("generate").disabled=false;$("generate").textContent="✦ Generate video";saveHistory({model:cfg.label,prompt:lastJob.prompt,url:videoUrl,created:new Date().toISOString()});setStatus("Video generated",true);return;}
    if(["failed","error","cancelled"].includes(state))throw new Error(status.error||status.error_message||"The model job failed: "+JSON.stringify(status).slice(0,500));
    pollTimer=setTimeout(()=>poll(id,model),4000);
  }catch(e){showError(e.message);setStatus("Generation failed");}}
$("generate").addEventListener("click",generate);$("retry").addEventListener("click",()=>{if(lastJob)generate();});$("newVideo").addEventListener("click",()=>showState("emptyState"));
function getHistory(){try{return JSON.parse(localStorage.getItem("psai_history")||"[]");}catch{return[];}}
function saveHistory(item){const list=[item,...getHistory()].slice(0,30);localStorage.setItem("psai_history",JSON.stringify(list));}
function renderHistory(){const list=getHistory();$("historyList").innerHTML=list.length?list.map(x=>'<article class="history-card"><b>'+escapeHtml(x.model)+'</b><p>'+escapeHtml(x.prompt)+'</p><small>'+new Date(x.created).toLocaleString()+'</small><p><a href="'+escapeAttr(x.url)+'" target="_blank" rel="noopener">Open generated video ↗</a></p></article>').join(""):'<p class="muted">No completed generations saved in this browser yet.</p>';}
function escapeHtml(s){return String(s).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
function escapeAttr(s){return escapeHtml(s);}
$("clearHistory").addEventListener("click",()=>{localStorage.removeItem("psai_history");renderHistory();});
setStatus("API key required");