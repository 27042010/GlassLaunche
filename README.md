# Glass Launcher

Launcher Android (Kotlin + Jetpack Compose) que imita a tela inicial do iPhone.

## O que tem

- Grid 4x6 paginado com parallax (a página que sai encolhe e desbota)
- Dock de vidro (Telefone, Mensagens, Navegador, Câmera, detectados automaticamente)
- Ícones em "squircle" com mola ao pressionar
- Animação de zoom saindo do ícone ao abrir um app
- Modo edição (segure em qualquer lugar): ícones tremendo + botão "−" para desinstalar + botão "OK"
- Deslize pra baixo = busca com teclado; deslize pra cima = biblioteca de apps
- Blur real do wallpaper atrás do overlay (Android 12+, com blur habilitado no aparelho)
- Botão Home volta pra primeira página e fecha overlays

## Como rodar

1. Abra a pasta `GlassLauncher` no Android Studio (Koala ou mais novo) e deixe o Gradle sincronizar
   (ele gera o wrapper sozinho).
2. Rode no celular/emulador (minSdk 26).
3. Aperte Home e escolha "Glass Launcher" como app inicial padrão.

## Limites conhecidos

- O vidro do dock e dos botões é desenhado (gradiente + borda de luz), não é blur do fundo por elemento.
  Blur real de backdrop por elemento no Android exige capturar o wallpaper, o que pede permissão de
  armazenamento. O blur real aqui é o da janela inteira, quando o overlay abre.
- Sem pastas, widgets nem arrastar ícones pra reordenar (próximos passos naturais).
- Código não foi compilado no ambiente onde foi gerado (sem Android SDK lá).
  Se o Gradle reclamar de algo, me manda o erro.
