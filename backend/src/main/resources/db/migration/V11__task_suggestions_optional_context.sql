alter table task_suggestions
    alter column category drop not null,
    alter column difficulty drop not null;
