let paginaRelatorio = 0;
    let filtrosAplicados = new URLSearchParams();
    let consultaAtual = 0;
    let operacaoEmAndamento = false;
    let inventario = [];
    let consultaEstoque = 0;
    let linhasRelatorio = [];
    let paginaExportada = 0;
    const el = id => document.getElementById(id);
    const moeda = valor => Number(valor).toLocaleString('pt-BR', {style:'currency', currency:'BRL'});

    function obterToken() { return localStorage.getItem('meu_token_jwt') || localStorage.getItem('token'); }

    async function api(url, opcoes = {}) {
        const token = obterToken();
        if (!token) { fazerLogout(); throw new Error('Faça login para acessar o sistema.'); }
        const resposta = await fetch(url, {...opcoes, headers: {
            'Content-Type':'application/json', 'Authorization':'Bearer ' + token, ...opcoes.headers
        }});
        if (!resposta.ok) {
            if (resposta.status === 401) {
                localStorage.removeItem('token');
                localStorage.removeItem('meu_token_jwt');
                window.location.replace('/login.html?expirada=1');
                throw new Error('Sessão expirada. Faça login novamente.');
            }
            const texto = await resposta.text();
            let mensagem = texto;
            try {
                const erro = JSON.parse(texto);
                mensagem = erro.mensagem || erro.message || texto;
                if (erro.erros) mensagem += ' ' + erro.erros.join('; ');
            } catch (_) { /* A API anterior também devolvia texto simples. */ }
            throw new Error(mensagem || 'Não foi possível concluir a operação.');
        }
        return resposta.status === 204 ? null : resposta.json();
    }

    function celula(linha, texto, classe) {
        const td = document.createElement('td');
        td.textContent = texto ?? 'Não registrado';
        if (classe) td.className = classe;
        linha.appendChild(td);
        return td;
    }
    function mensagemTabela(id, texto, colunas) {
        const corpo = el(id); corpo.replaceChildren();
        const linha = document.createElement('tr');
        celula(linha, texto).colSpan = colunas;
        corpo.appendChild(linha);
    }
    function preencherSelect(id, dados, rotulo, primeiro) {
        const select = el(id), selecionado = select.value;
        select.replaceChildren(new Option(primeiro, ''));
        dados.forEach(dado => select.add(new Option(rotulo(dado), dado.id)));
        select.value = selecionado;
        if (select.selectedIndex < 0) select.selectedIndex = 0;
    }
    function botao(texto, acao) {
        const b = document.createElement('button');
        b.textContent = texto; b.addEventListener('click', acao); return b;
    }

    function notificar(mensagem, erro = false) {
        const aviso = el('notificacao');
        aviso.textContent = mensagem;
        aviso.className = erro ? 'erro' : '';
        aviso.hidden = false;
        document.querySelectorAll('dialog[open] .dialog-feedback').forEach(p => p.textContent = mensagem);
    }
    function abrirDialogo(id) {
        if (!obterToken()) { fazerLogout(); return; }
        el(id).querySelector('.dialog-feedback').textContent = '';
        el(id).showModal();
    }
    function fecharDialogo(id) {
        if (!operacaoEmAndamento) el(id).close();
    }
    function renderizarInventario() {
        const termo = el('busca-itens').value.trim().toLocaleLowerCase('pt-BR');
        const filtro = el('filtro-estoque').value;
        const itens = inventario.filter(i => `${i.codigo} ${i.nome}`.toLocaleLowerCase('pt-BR').includes(termo)
            && (filtro === 'todos' || (filtro === 'baixo' ? i.quantidade < 20 : i.quantidade === 0)));
        const corpo = el('tabela-itens'); corpo.replaceChildren();
        itens.forEach(item => {
            const linha = document.createElement('tr');
            celula(linha, item.codigo); celula(linha, item.nome);
            celula(linha, item.quantidade + ' un.', item.quantidade < 20 ? 'estoque-baixo' : '');
            celula(linha, moeda(item.valor));
            const acoes = celula(linha, '');
            const editar = botao('Editar valor', () => editarValor(item.id, item.valor));
            const remover = botao('Remover', () => removerItem(item.id));
            for (const b of [editar, remover]) {
                b.dataset.mutacao = ''; b.disabled = operacaoEmAndamento;
                b.setAttribute('aria-label', `${b.textContent}: ${item.nome}`);
            }
            acoes.append(editar, document.createTextNode(' '), remover);
            corpo.appendChild(linha);
        });
        if (!itens.length) mensagemTabela('tabela-itens', inventario.length ? 'Nenhum brinde corresponde aos filtros.' : 'Nenhum brinde cadastrado. Use “Novo brinde” para começar.', 5);
        el('contagem-itens').textContent = `${itens.length} de ${inventario.length} produto(s)`;
    }
    async function carregarDados() {
        const consulta = ++consultaEstoque;
        el('estado-dados').textContent = 'Atualizando...';
        el('estado-dados').dataset.status = '';
        try {
            // Uma única leitura mantém cartões e tabela consistentes entre si.
            const itens = await api('/itens');
            if (consulta !== consultaEstoque) return;
            inventario = itens;
            el('total-brindes').textContent = itens.reduce((s, i) => s + i.quantidade, 0).toLocaleString('pt-BR');
            el('valor-total').textContent = moeda(itens.reduce((s, i) => s + i.quantidade * Math.round(i.valor * 100), 0) / 100);
            el('total-produtos').textContent = itens.length;
            el('total-baixos').textContent = itens.filter(i => i.quantidade < 20).length;
            preencherSelect('mov-id-item', itens, i => `${i.codigo} - ${i.nome} (Estoque: ${i.quantidade})`, 'Selecione o brinde...');
            renderizarInventario();
            el('estado-dados').textContent = 'Atualizado às ' + new Date().toLocaleTimeString('pt-BR', {hour:'2-digit', minute:'2-digit'});
            el('estado-dados').dataset.status = 'ok';
        } catch (erro) {
            if (consulta !== consultaEstoque) return;
            inventario = [];
            preencherSelect('mov-id-item', [], i => i.nome, 'Não foi possível carregar os brindes');
            mensagemTabela('tabela-itens', erro.message, 5);
            ['total-brindes','valor-total','total-produtos','total-baixos'].forEach(id => el(id).textContent = '—');
            el('contagem-itens').textContent = 'Dados indisponíveis';
            el('estado-dados').textContent = 'Falha ao atualizar';
            el('estado-dados').dataset.status = 'erro';
        }
    }
    async function executarOperacao(acao) {
        if (operacaoEmAndamento) return;
        operacaoEmAndamento = true;
        document.querySelectorAll('[data-mutacao]').forEach(b => b.disabled = true);
        document.querySelectorAll('dialog[open] .dialog-feedback').forEach(p => p.textContent = 'Salvando...');
        try {
            await acao();
            document.querySelectorAll('dialog[open]').forEach(d => d.close());
            notificar('Operação concluída com sucesso.');
            await carregarDados();
        } catch (erro) { notificar(erro.message, true); }
        finally {
            operacaoEmAndamento = false;
            document.querySelectorAll('[data-mutacao]').forEach(b => b.disabled = false);
        }
    }
    function cadastrarItem() {
        const dados = {codigo:el('novo-codigo').value.trim(), nome:el('novo-nome').value.trim(),
            quantidade:Number(el('novo-qtd').value), valor:Number(el('novo-valor').value)};
        if (!dados.codigo || !dados.nome || el('novo-qtd').value === '' || el('novo-valor').value === ''
            || !Number.isInteger(dados.quantidade) || dados.quantidade < 0 || !Number.isFinite(dados.valor) || dados.valor < 0) {
            notificar('Preencha código, nome, quantidade inteira não negativa e valor não negativo.', true); return;
        }
        executarOperacao(async () => {
            await api('/itens', {method:'POST', body:JSON.stringify(dados)});
            ['novo-codigo','novo-nome','novo-qtd','novo-valor'].forEach(id => el(id).value = '');
            el('novo-qtd').value = '0';
        });
    }
    function registrarMovimentacao() {
        const itemId = Number(el('mov-id-item').value), quantidade = Number(el('mov-qtd').value);
        if (!itemId || !Number.isInteger(quantidade) || quantidade <= 0) {
            notificar('Selecione o item e informe uma quantidade inteira positiva.', true); return;
        }
        executarOperacao(async () => {
            await api('/movimentacoes', {method:'POST', body:JSON.stringify({item:{id:itemId},
                tipo:el('mov-tipo').value, quantidade, observacao:el('mov-observacao').value.trim() || null})});
            el('mov-qtd').value = ''; el('mov-observacao').value = '';

        });
    }
    function removerItem(id) {
        const item = inventario.find(i => i.id === id);
        if (!item || operacaoEmAndamento) return;
        if (confirm(`Excluir “${item.nome}” do inventário?\n\nSaldo atual: ${item.quantidade} unidade(s). Esse saldo deixará de compor os totais.\nO histórico será preservado e novas movimentações serão bloqueadas. O código continuará reservado.`)) {
            executarOperacao(() => api(`/itens/${id}`, {method:'DELETE'}));
        }
    }
    function editarValor(id, atual) {
        el('edicao-id').value = id;
        el('edicao-valor').value = atual;
        el('nome-edicao').textContent = inventario.find(i => i.id === id)?.nome || '';
        abrirDialogo('dialogo-edicao');
    }
    function atualizarRodape() {
        el('area-autenticacao').replaceChildren(obterToken()
            ? botao('Desconectar (Sair)', fazerLogout)
            : botao('Acesso Gerencial (Login)', () => window.location.href = '/login.html'));
    }
    function fazerLogout() {
        localStorage.removeItem('token'); localStorage.removeItem('meu_token_jwt');
        window.location.href = '/login.html';
    }

    async function abrirAba(aba) {
        const relatorio = aba === 'relatorio';
        el('painel-estoque').hidden = relatorio; el('painel-relatorio').hidden = !relatorio;
        el('aba-estoque').setAttribute('aria-pressed', String(!relatorio));
        el('aba-relatorio').setAttribute('aria-pressed', String(relatorio));
        if (!relatorio) { carregarDados(); return; }
        try {
            const [itens, responsaveis] = await Promise.all([api('/itens/historico'), api('/movimentacoes/responsaveis')]);
            preencherSelect('filtro-item', itens, i => `${i.codigo} - ${i.nome}${i.ativo === false ? ' (excluído)' : ''}`, 'Todos');
            preencherSelect('filtro-responsavel', responsaveis, u => u.login, 'Todos');
            aplicarFiltros();
        } catch (erro) {
            ++consultaAtual;
            linhasRelatorio = [];
            el('exportar-csv').disabled = true;
            el('pagina-info').textContent = '';
            mensagemTabela('tabela-movimentacoes', erro.message, 10);
            el('status-relatorio').textContent = erro.message;
            el('pagina-anterior').disabled = true; el('pagina-proxima').disabled = true;
        }
    }
    function aplicarFiltros() {
        const inicio = el('filtro-inicio').value, fim = el('filtro-fim').value;
        if (inicio && fim && inicio > fim) {
            ++consultaAtual; linhasRelatorio = []; el('exportar-csv').disabled = true;
            el('pagina-anterior').disabled = true; el('pagina-proxima').disabled = true;
            el('pagina-info').textContent = '';
            mensagemTabela('tabela-movimentacoes', 'Ajuste o período para consultar.', 10);
            el('status-relatorio').textContent = 'A data inicial não pode ser posterior à final.'; return;
        }
        filtrosAplicados = new URLSearchParams();
        const campos = {dataInicial:inicio, dataFinal:fim, responsavelId:el('filtro-responsavel').value,
            tipo:el('filtro-tipo').value, itemId:el('filtro-item').value};
        Object.entries(campos).forEach(([chave, valor]) => { if (valor) filtrosAplicados.set(chave, valor); });
        consultarRelatorio(0);
    }
    function limparFiltros() { el('form-filtros').reset(); aplicarFiltros(); }
    async function consultarRelatorio(pagina) {
        const consulta = ++consultaAtual;
        linhasRelatorio = [];
        el('exportar-csv').disabled = true;
        el('status-relatorio').textContent = 'Carregando movimentações...';
        el('pagina-anterior').disabled = true; el('pagina-proxima').disabled = true;
        mensagemTabela('tabela-movimentacoes', 'Carregando...', 10);
        const parametros = new URLSearchParams(filtrosAplicados);
        parametros.set('pagina', pagina); parametros.set('tamanho', '20');
        try {
            const dados = await api('/movimentacoes/relatorio?' + parametros);
            if (consulta !== consultaAtual) return; // Impede uma resposta antiga de sobrescrever filtros novos.
            paginaRelatorio = dados.pagina;
            paginaExportada = dados.pagina;
            linhasRelatorio = dados.conteudo;
            el('exportar-csv').disabled = !linhasRelatorio.length;
            const corpo = el('tabela-movimentacoes'); corpo.replaceChildren();
            dados.conteudo.forEach(m => {
                const linha = document.createElement('tr');
                // LocalDateTime já representa horário de São Paulo; não converter pelo fuso do navegador.
                const partes = m.dataMovimentacao ? m.dataMovimentacao.split('T') : [];
                celula(linha, m.id);
                celula(linha, partes[0] ? partes[0].split('-').reverse().join('/') : null);
                celula(linha, partes[1] ? partes[1].substring(0,8) : null);
                celula(linha, m.tipo === 'SAIDA' ? 'SAÍDA' : m.tipo, m.tipo === 'ENTRADA' ? 'tipo-entrada' : 'tipo-saida');
                celula(linha, `${m.item.codigo} - ${m.item.nome}`);
                celula(linha, m.quantidade);
                celula(linha, m.responsavel ? m.responsavel.login : 'Responsável não registrado');
                celula(linha, m.observacao || '—'); celula(linha, m.estoqueAnterior); celula(linha, m.estoquePosterior);
                corpo.appendChild(linha);
            });
            if (!dados.conteudo.length) mensagemTabela('tabela-movimentacoes', 'Nenhuma movimentação encontrada.', 10);
            el('status-relatorio').textContent = `${dados.totalElementos} movimentação(ões) encontrada(s).`;
            el('pagina-info').textContent = dados.totalPaginas ? `Página ${dados.pagina + 1} de ${dados.totalPaginas}` : 'Sem resultados';
            el('pagina-anterior').disabled = dados.pagina <= 0;
            el('pagina-proxima').disabled = dados.pagina + 1 >= dados.totalPaginas;
        } catch (erro) {
            if (consulta !== consultaAtual) return;
            el('status-relatorio').textContent = erro.message;
            el('pagina-info').textContent = '';
            mensagemTabela('tabela-movimentacoes', erro.message, 10);
        }
    }
    el('form-filtros').addEventListener('submit', evento => { evento.preventDefault(); aplicarFiltros(); });
    // Aspas, separador e prevenção de fórmulas ao abrir o CSV em planilhas.
    function campoCSV(valor) {
        let texto = String(valor ?? 'Não registrado');
        if (/^[\s]*[=+@-]/.test(texto)) texto = "'" + texto;
        return '"' + texto.replaceAll('"', '""') + '"';
    }
    function exportarPaginaCSV() {
        if (!linhasRelatorio.length) return;
        const linhas = [['ID','Data e hora (Brasília)','Tipo','Código','Produto','Quantidade','Responsável','Observação','Saldo anterior','Saldo posterior'],
            ...linhasRelatorio.map(m => [m.id,m.dataMovimentacao,m.tipo,m.item.codigo,m.item.nome,m.quantidade,
                m.responsavel?.login,m.observacao,m.estoqueAnterior,m.estoquePosterior])];
        const csv = '\uFEFF' + linhas.map(l => l.map(campoCSV).join(';')).join('\r\n');
        const url = URL.createObjectURL(new Blob([csv], {type:'text/csv;charset=utf-8'}));
        const link = document.createElement('a'); link.href = url;
        link.download = `movimentacoes-pagina-${paginaExportada + 1}.csv`;
        document.body.appendChild(link); link.click(); link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
    el('busca-itens').addEventListener('input', renderizarInventario);
    el('filtro-estoque').addEventListener('change', renderizarInventario);
    el('form-cadastro').addEventListener('submit', e => { e.preventDefault(); cadastrarItem(); });
    el('form-movimento').addEventListener('submit', e => { e.preventDefault(); registrarMovimentacao(); });
    el('form-edicao').addEventListener('submit', e => {
        e.preventDefault();
        const id = Number(el('edicao-id').value), valor = Number(el('edicao-valor').value);
        executarOperacao(() => api(`/itens/${id}`, {method:'PUT', body:JSON.stringify({valor})}));
    });
    document.querySelectorAll('dialog').forEach(d => d.addEventListener('cancel', e => {
        if (operacaoEmAndamento) e.preventDefault();
    }));
    window.addEventListener('storage', e => {
        if ((e.key === 'meu_token_jwt' || e.key === 'token' || e.key === null) && !obterToken()) fazerLogout();
    });
    if (!obterToken()) window.location.replace('/login.html');
    else { atualizarRodape(); carregarDados(); }
