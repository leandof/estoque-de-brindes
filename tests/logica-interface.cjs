// Testes de lógica JavaScript com DOM mínimo e HTTP simulado.
// Não substituem testes visuais nem integração com o servidor Java.
// Executar: node tests/logica-interface.cjs
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const elementos = new Map();
function elemento(id) {
    if (!elementos.has(id)) elementos.set(id, {value:'',textContent:'',dataset:{},hidden:false,
        addEventListener(){}, replaceChildren(){}, setAttribute(){}, appendChild(){},
        querySelector(){return elemento(id+'-feedback');}, showModal(){this.open=true;},close(){this.open=false;}});
    return elementos.get(id);
}
const dados = new Map();
let destino = '', confirmacao = '', chamadas = [], responder;
const ctx = vm.createContext({console,URL,URLSearchParams,Blob,setTimeout,Option:function(){},
    document:{getElementById:elemento,querySelectorAll:()=>[],createElement:()=>elemento('novo')},
    localStorage:{getItem:k=>dados.get(k)||null,setItem:(k,v)=>dados.set(k,v),removeItem:k=>dados.delete(k)},
    window:{location:{replace:url=>destino=url},addEventListener(){}},
    confirm:msg=>{confirmacao=msg;return true;},
    fetch:async (url,options)=>{chamadas.push({url,options});return responder(url,options);}
});
vm.runInContext(fs.readFileSync(path.join(__dirname,'../src/main/resources/static/js/index.js'),'utf8'),ctx);
const run = code => vm.runInContext(code,ctx);
(async()=>{
    assert.equal(destino,'/login.html');
    assert.equal(run(`campoCSV('=HYPERLINK("x")')`),`"'=HYPERLINK(""x"")"`);
    assert.equal(run(`campoCSV('Caneca; "A"')`),'"Caneca; ""A"""');
    assert.equal(run('campoCSV(null)'),'"Não registrado"');
    dados.set('meu_token_jwt','teste');
    responder=()=>({ok:false,status:403,text:async()=>'{"mensagem":"Sem permissão"}'});
    await assert.rejects(run("api('/itens')"),/Sem permissão/);
    assert.equal(dados.get('meu_token_jwt'),'teste');
    responder=()=>({ok:false,status:401});
    await assert.rejects(run("api('/itens')"),/Sessão expirada/);
    assert.equal(dados.has('meu_token_jwt'),false);
    assert.equal(destino,'/login.html?expirada=1');
    dados.set('meu_token_jwt','teste');
    responder=()=>({ok:true,status:204});
    assert.equal(await run("api('/itens/1',{method:'DELETE'})"),null);
    assert.equal(chamadas.at(-1).options.headers.Authorization,'Bearer teste');
    // Confirmação explicita saldo e preservação de histórico; a rota permanece DELETE.
    run("inventario=[{id:7,nome:'Caneca',quantidade:30}]; executarOperacao = acao => acao(); removerItem(7)");
    await new Promise(r=>setImmediate(r));
    assert.match(confirmacao,/30 unidade/);
    assert.match(confirmacao,/histórico será preservado/);
    assert.equal(chamadas.at(-1).url,'/itens/7');
    assert.equal(chamadas.at(-1).options.method,'DELETE');
    // Respostas tardias de relatórios não devem reabilitar a exportação.
    run("consultaAtual=1; linhasRelatorio=[{}]");
    elemento('filtro-inicio').value='2026-11-01';
    elemento('filtro-fim').value='2026-10-01';
    run('aplicarFiltros()');
    assert.equal(elemento('exportar-csv').disabled,true);
    assert.equal(run('linhasRelatorio.length'),0);
    assert.equal(run('consultaAtual'),2);
    console.log('PASS: CSV seguro e escape, login ausente, 401, 403, DELETE 204, exclusão com saldo e período inválido.');
})().catch(e=>{console.error(e);process.exit(1)});
