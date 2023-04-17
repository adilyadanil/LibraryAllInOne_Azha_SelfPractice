-- QUERIES FOR THE PROJECT

select name, author, isbn  from books
where name = 'Dexter Zubat';


select full_name, email, password, user_group_id, start_date,end_date,address
from users where full_name = 'Allen Jacobs';