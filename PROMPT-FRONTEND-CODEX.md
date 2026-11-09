# Alteração: funcionalidades controladas por flags no frontend

Adapte o frontend existente: a API agora gerencia somente as flags. O endpoint GET /demo/home foi removido. Remova suas chamadas e os tipos DemoHome, banners e offers vindos da API.

Consulte GET /flags na base configurada (padrão http://localhost:8080/api/v1). A resposta é um array de objetos com key, name, description, enabled, createdAt e updatedAt. Converta em um mapa por chave e use o estado enabled nos condicionais:

```typescript
const enabled = (key: string) => flags.some(flag => flag.key === key && flag.enabled);

if (enabled('welcome-banner')) {
  // Exibir o componente de boas-vindas definido no frontend.
}
if (enabled('special-offers')) {
  // Exibir o componente de ofertas definido no frontend.
}
```

O frontend define o conteúdo e os componentes. Preserve a demonstração de banner e ofertas como exemplos locais. A API não fornece mensagens, ofertas nem aplica descontos. Cada nova funcionalidade precisa de um condicional no código associado à sua chave; cadastrar uma flag não cria código automaticamente.

Preserve o gerenciamento de flags. Para ativar/desativar, envie PATCH /flags/{key}/status com Content-Type: application/json e corpo {"enabled":true} ou {"enabled":false}. Só atualize o estado após sucesso; use a flag retornada e compartilhe esse estado com a demonstração para refletir a alteração sem recarregar a página. Após excluir uma flag, remova-a também do estado local.

Flags ausentes são consideradas desativadas. Durante carregamento inicial ou falha na consulta, mantenha funcionalidades condicionais ocultas e mostre carregamento/erro com opção de tentar novamente. Inclua atualização manual para buscar alterações feitas em outra aba ou cliente. Não prometa sincronização automática entre clientes.

welcome-banner e special-offers são criadas desativadas somente em instalações novas. Em instalações existentes, permita cadastrá-las pelo formulário. Verifique que alternar cada flag muda apenas seu componente e que excluir a flag oculta o componente.

Condicionais no frontend controlam apresentação. Permissões e regras de negócio sensíveis devem ser verificadas no backend responsável por essas operações.