create table if not exists evento_calendario (
  id bigint primary key auto_increment,
  usuario_id bigint not null,
  asignatura_id bigint null,
  titulo varchar(200) not null,
  descripcion text null,
  fecha_inicio date not null,
  hora_inicio time null,
  tipo varchar(20) not null,
  completado boolean not null default false,
  creado_en datetime not null,
  actualizado_en datetime not null,
  constraint fk_evento_calendario_usuario foreign key (usuario_id) references usuario(id),
  constraint fk_evento_calendario_asignatura foreign key (asignatura_id) references asignatura(id)
);

set @idx_exists := (
  select count(1)
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'evento_calendario'
    and index_name = 'idx_evento_calendario_usuario_fecha'
);

set @sql := if(
  @idx_exists = 0,
  'create index idx_evento_calendario_usuario_fecha on evento_calendario (usuario_id, fecha_inicio)',
  'select 1'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

