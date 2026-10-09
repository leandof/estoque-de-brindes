let loginEmAndamento = false;
async function fazerLogin(event) {
    event.preventDefault();
    if (loginEmAndamento) return;
    const mensagem = document.getElementById('msg-login');
    const botao = event.target.querySelector('button[type="submit"]');
    mensagem.style.display = 'none';
    loginEmAndamento = true;
    botao.disabled = true;
    botao.textContent = 'Entrando...';
    try {
        const response = await fetch('/login', {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({login:document.getElementById('loginUser').value.trim(), senha:document.getElementById('loginSenha').value})
        });
        if (!response.ok) throw new Error(response.status === 401 ? 'Usuário ou senha inválidos.' : 'Não foi possível entrar agora. Tente novamente em instantes.');
        const dados = await response.json();
        if (!dados.token) throw new Error('Resposta de login inválida. Tente novamente.');
        localStorage.removeItem('token');
        localStorage.setItem('meu_token_jwt', dados.token);
        window.location.replace('/');
    } catch (erro) {
        mensagem.textContent = erro instanceof TypeError ? 'Não foi possível conectar ao sistema. Confira sua conexão e tente novamente.' : erro.message;
        mensagem.style.display = 'block';
    } finally {
        loginEmAndamento = false;
        botao.disabled = false;
        botao.textContent = 'Entrar no sistema';
    }
}
if (new URLSearchParams(window.location.search).has('expirada')) {
    const mensagem = document.getElementById('msg-login');
    mensagem.textContent = 'Sua sessão expirou. Entre novamente para continuar.';
    mensagem.style.display = 'block';
}
