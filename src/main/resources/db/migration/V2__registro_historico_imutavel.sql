-- O histórico de operações críticas só aceita inserções: UPDATE e DELETE são bloqueados no próprio banco.
CREATE OR REPLACE FUNCTION bloquear_alteracao_registro_historico() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'registro_historico é imutável: operação % não permitida', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registro_historico_imutavel
    BEFORE UPDATE OR DELETE ON registro_historico
    FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao_registro_historico();
