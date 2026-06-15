create table if not exists unidad_tematica (
  id bigint primary key auto_increment,
  asignatura_id bigint not null,
  titulo varchar(200) not null,
  orden int not null default 0,
  trimestre int null,
  creado_en datetime not null,
  actualizado_en datetime not null,
  constraint fk_unidad_tematica_asignatura foreign key (asignatura_id) references asignatura(id)
);

set @idx_ut_exists := (
  select count(1)
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'unidad_tematica'
    and index_name = 'idx_unidad_tematica_asignatura_orden'
);
set @sql := if(
  @idx_ut_exists = 0,
  'create index idx_unidad_tematica_asignatura_orden on unidad_tematica (asignatura_id, orden)',
  'select 1'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @col_exists := (
  select count(1)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'tema'
    and column_name = 'unidad_tematica_id'
);
set @sql := if(
  @col_exists = 0,
  'alter table tema add column unidad_tematica_id bigint null',
  'select 1'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @idx_tema_exists := (
  select count(1)
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'tema'
    and index_name = 'idx_tema_unidad_tematica'
);
set @sql := if(
  @idx_tema_exists = 0,
  'create index idx_tema_unidad_tematica on tema (unidad_tematica_id)',
  'select 1'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @fk_exists := (
  select count(1)
  from information_schema.referential_constraints
  where constraint_schema = database()
    and table_name = 'tema'
    and constraint_name = 'fk_tema_unidad_tematica'
);
set @sql := if(
  @fk_exists = 0,
  'alter table tema add constraint fk_tema_unidad_tematica foreign key (unidad_tematica_id) references unidad_tematica(id)',
  'select 1'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

