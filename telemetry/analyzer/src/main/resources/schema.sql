DROP TABLE IF EXISTS scenario_actions CASCADE;
DROP TABLE IF EXISTS scenario_conditions CASCADE;
DROP TABLE IF EXISTS actions CASCADE;
DROP TABLE IF EXISTS conditions CASCADE;
DROP TABLE IF EXISTS sensors CASCADE;
DROP TABLE IF EXISTS scenarios CASCADE;
DROP FUNCTION IF EXISTS check_hub_id CASCADE;

CREATE TABLE scenarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hub_id VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    UNIQUE(hub_id, name)
);

CREATE TABLE sensors (
    id VARCHAR PRIMARY KEY,
    hub_id VARCHAR NOT NULL
);

CREATE TABLE conditions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR NOT NULL,
    operation VARCHAR NOT NULL,
    value INTEGER
);

CREATE TABLE actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR NOT NULL,
    value INTEGER
);

CREATE TABLE scenario_conditions (
    scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR REFERENCES sensors(id) ON DELETE CASCADE,
    condition_id BIGINT REFERENCES conditions(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, sensor_id, condition_id)
);

CREATE TABLE scenario_actions (
    scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR REFERENCES sensors(id) ON DELETE CASCADE,
    action_id BIGINT REFERENCES actions(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, sensor_id, action_id)
);

CREATE OR REPLACE FUNCTION check_hub_id()
RETURNS TRIGGER AS
$$
BEGIN
    IF (SELECT hub_id FROM scenarios WHERE id = NEW.scenario_id) != (SELECT hub_id FROM sensors WHERE id = NEW.sensor_id) THEN
        RAISE EXCEPTION 'Hub IDs do not match for scenario_id % and sensor_id %', NEW.scenario_id, NEW.sensor_id;
    END IF;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER tr_bi_scenario_conditions_hub_id_check
BEFORE INSERT ON scenario_conditions
FOR EACH ROW
EXECUTE FUNCTION check_hub_id();

CREATE TRIGGER tr_bi_scenario_actions_hub_id_check
BEFORE INSERT ON scenario_actions
FOR EACH ROW
EXECUTE FUNCTION check_hub_id();