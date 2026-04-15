# Atividade 2 – Manutenção Corretiva e Evolutiva

## Bugs Corrigidos

---

### Bug 1 — LoanManager.returnBook(): retorno silencioso quando empréstimo não existe
**Arquivo:** `src/LoanManager.java`  
**Método:** `returnBook()`

**Como reproduzir (antes da correção):**
1. Execute o sistema: `java -cp src Main`
2. Escolha opção 4 (Return book)
3. Informe um Loan ID inexistente, ex: `999`
4. **Resultado antes:** sistema imprime "Return completed" sem fazer nada.
5. **Resultado depois:** sistema lança erro "Loan not found: 999"

**O que foi mudado:**  
Substituído o `return` silencioso por `throw new RuntimeException("Loan not found: " + loanId)`.

---

### Bug 2 — LoanManager.borrowBook(): canal SMS cria empréstimo duplicado
**Arquivo:** `src/LoanManager.java`  
**Método:** `borrowBook()`

**Como reproduzir (antes da correção):**
1. Execute o sistema: `java -cp src Main`
2. Escolha opção 3 (Borrow book)
3. Informe userId=1, bookId=1, channel=`sms`
4. Escolha opção 8 (List loans)
5. **Resultado antes:** 2 registros de empréstimo criados para o mesmo livro/usuário.
6. **Resultado depois:** apenas 1 registro criado.

**O que foi mudado:**  
Removido o bloco `if ("sms".equals(channel)) { LegacyDatabase.addLoanData(...) }` que criava um segundo empréstimo "de sincronização".

---

### Bug 3 — LoanManager.returnBook(): multa subtrai dívida em vez de somar
**Arquivo:** `src/LoanManager.java`  
**Método:** `returnBook()`

**Como reproduzir (antes da correção):**
1. Crie um empréstimo com data de vencimento anterior à data de devolução
2. Processe a devolução
3. Verifique o relatório (opção 6) para ver a dívida do usuário
4. **Resultado antes:** dívida do usuário diminui (ou vai negativa).
5. **Resultado depois:** dívida do usuário aumenta corretamente.

**O que foi mudado:**  
Trocado `debt = debt - fine` por `debt = debt + fine`.

---

## Funcionalidade Nova — Histórico de Empréstimos por Usuário

**Opção no menu:** `10 - Loan history by user`  
**Arquivo modificado:** `src/LoanManager.java` (novo método `listLoansByUser()`)  
**Arquivo modificado:** `src/LibrarySystem.java` (novo handler + nova opção no menu)

**Como usar:**
1. Execute `java -cp src Main`
2. Escolha opção `10`
3. Informe o User ID (ex: `1`)
4. O sistema lista todos os empréstimos do usuário com ID, livro, datas, status e multa.

**Impactos e riscos:**
- Não altera nenhuma estrutura de dados existente.
- Não modifica fluxos de empréstimo, devolução ou relatório.
- Apenas lê dados já existentes em `LegacyDatabase.getLoans()`.
- Risco: nenhum. É somente leitura.

---

## Evidências de Execução

### Compilação