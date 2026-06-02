CREATE TABLE IF NOT EXISTS bulk_job (
   id UUID PRIMARY KEY,
   jira_reference TEXT NOT NULL,
   number_of_roles INTEGER NOT NULL,
   number_of_users INTEGER NOT NULL,
   status TEXT NOT NULL,
   requested_by TEXT NOT NULL,
   request_date_time TIMESTAMP with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS bulk_job_item ( 
   id UUID PRIMARY KEY,
   username TEXT NOT NULL,
   rolename TEXT NOT NULL,
   status TEXT NOT NULL, 
   result TEXT,
   bulk_job_id UUID NOT NULL,
   constraint fk_bulk_job foreign key (bulk_job_id) REFERENCES bulk_job (id)
);
