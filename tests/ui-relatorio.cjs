// Teste da interface com API simulada; não acessa nem modifica o banco real.
// Dependência local opcional: npm install --no-save playwright
// Execução: node tests/ui-relatorio.cjs
const { chromium } = require('playwright');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const tmp = require('node:os').tmpdir();

(async () => {
    const browser = await chromium.launch({headless:true, ...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE ? {executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE} : {})});
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', e => errors.push(e.message));
        const html = readFileSync(path.join(__dirname, '../src/main/resources/static/index.html'), 'utf8');
        const item = {id:1, codigo:'CAN-01', nome:'<img src=x onerror="window.xss=true">', quantidade:80, valor:25};
        const movimentos = [
            {id:2, dataMovimentacao:'2026-10-05T09:15:30', tipo:'SAIDA', item, quantidade:20,
                responsavel:{id:2,login:'maria'}, observacao:'Evento', estoqueAnterior:100,estoquePosterior:80},
            {id:1, dataMovimentacao:'2026-10-05T08:30:00', tipo:'ENTRADA', item, quantidade:100,
                responsavel:null, observacao:null, estoqueAnterior:null,estoquePosterior:null}
        ];
        await page.route('http://estoque.test/**', async route => {
            const url = new URL(route.request().url());
            let data;
            if (url.pathname === '/') return route.fulfill({contentType:'text/html',body:html});
            if (/^\/(css|js|assets)\//.test(url.pathname)) {
                const file = path.join(__dirname, '../src/main/resources/static', url.pathname);
                const contentType = url.pathname.endsWith('.css') ? 'text/css' : url.pathname.endsWith('.js') ? 'application/javascript' : 'image/jpeg';
                return route.fulfill({contentType,body:readFileSync(file)});
            }
            if (url.pathname === '/itens' || url.pathname === '/itens/historico') data=[item];
            else if (url.pathname === '/itens/relatorio') data={totalDeBrindesCadastrados:80,valorTotalArmazenado:2000};
            else if (url.pathname === '/movimentacoes/responsaveis') data=[{id:2,login:'maria'}];
            else if (url.pathname === '/movimentacoes/relatorio') {
                let linhas=movimentos.filter(m => !url.searchParams.get('tipo') || m.tipo === url.searchParams.get('tipo'));
                if(url.searchParams.get('responsavelId')) linhas=linhas.filter(m => String(m.responsavel?.id) === url.searchParams.get('responsavelId'));
                if(url.searchParams.get('dataInicial') === '2030-01-01') linhas=[];
                data={conteudo:linhas,pagina:0,tamanho:20,totalElementos:linhas.length,totalPaginas:linhas.length?1:0};
            } else return route.fulfill({status:404,body:''});
            return route.fulfill({contentType:'application/json',body:JSON.stringify(data)});
        });
        await page.addInitScript(() => localStorage.setItem('meu_token_jwt','token-simulado'));
        await page.goto('http://estoque.test/');
        await page.waitForFunction(() => document.querySelector('#total-brindes').textContent === '80');
        assert.equal(await page.locator('#tabela-itens img').count(),0);
        await page.locator('#busca-itens').fill('inexistente');
        await page.getByText('Nenhum brinde corresponde aos filtros.',{exact:true}).waitFor();
        await page.locator('#busca-itens').fill('CAN-01');
        assert.equal(await page.locator('#tabela-itens tr').count(),1);
        await page.getByRole('button',{name:'+ Novo brinde',exact:true}).click();
        assert.equal(await page.locator('#dialogo-cadastro').evaluate(d => d.open),true);
        await page.keyboard.press('Escape');
        await page.screenshot({path:process.env.ESTOQUE_SCREENSHOT || path.join(tmp, 'estoque-desktop.png'),fullPage:true});
        await page.getByRole('button',{name:'Relatório de Movimentações',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('2 movimentação'));
        assert.match(await page.locator('#tabela-movimentacoes').innerText(), /Responsável não registrado/);
        assert.equal(await page.locator('#tabela-movimentacoes img').count(),0);
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button',{name:'Baixar página em CSV',exact:true}).click();
        const download = await downloadPromise;
        const csv = readFileSync(await download.path(),'utf8');
        assert.match(csv,/CAN-01/);
        assert.match(csv,/Não registrado/);
        await page.locator('#filtro-tipo').selectOption('SAIDA');
        await page.locator('#filtro-responsavel').selectOption('2');
        await page.getByRole('button',{name:'Filtrar',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('1 movimentação'));
        assert.equal(await page.locator('#tabela-movimentacoes tr').count(),1);
        await page.locator('#filtro-inicio').fill('2030-01-01');
        await page.getByRole('button',{name:'Filtrar',exact:true}).click();
        await page.getByText('Nenhuma movimentação encontrada.',{exact:true}).waitFor();
        await page.getByRole('button',{name:'Limpar filtros',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('2 movimentação'));
        assert.equal(await page.locator('#filtro-tipo').inputValue(),'');
        await page.locator('#filtro-inicio').fill('2026-11-01');
        await page.locator('#filtro-fim').fill('2026-10-01');
        await page.getByRole('button',{name:'Filtrar',exact:true}).click();
        await page.getByText('A data inicial não pode ser posterior à final.',{exact:true}).waitFor();
        await page.getByRole('button',{name:'Limpar filtros',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('2 movimentação'));
        await page.screenshot({path:process.env.RELATORIO_SCREENSHOT || path.join(tmp, 'relatorio-desktop.png'),fullPage:true});
        await page.setViewportSize({width:390,height:844});
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),true);
        await page.screenshot({path:path.join(tmp, 'relatorio-mobile.png'),fullPage:true});
        assert.equal(await page.evaluate(() => window.xss === true),false);
        assert.deepEqual(errors,[]);
        console.log('PASS: abas, filtros, vazio, limpar, datas inválidas, legado, texto seguro e largura móvel.');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
