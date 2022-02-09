package com.yoann_bezard.controllers;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.yoann_bezard.dao.DaoFactory;
import com.yoann_bezard.dao.entiites.Utilisateur;
import com.yoann_bezard.dao.interfaces.UtilisateurInterface;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/utilisateur")
public class UtilisateurController {

        /**
         * Permet de dire bonjour à un administrateur.
         * @return
         */
        @Path("admin/hello")
        @GET
        public Response bonjourAdmin() {
                Response response = Response 
                        .ok("Bonjour administrateur.")
                        .build();
                return response;
        }

        /**
         * Afficher la liste des utilisateurs.
         * @return 
         */
        @Path("admin/liste")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAllUtilisateursBd() {
                DaoFactory daoFactory = new DaoFactory();
                UtilisateurInterface utilisateurInterface = daoFactory.getUtilisateurInterface();

                List<Utilisateur> listUtilisateurs = utilisateurInterface.findAllUtilisateurs();

                Response response = Response
                                .ok("Voici la liste des utilisateurs :" + listUtilisateurs)
                                .build();

                return response;
        }

        /**
         * Création d'un nouvel utilisateur.
         * @param user
         * @return
         */
        @Path("guest/create")
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response createUserBd( Utilisateur user ) {
                DaoFactory daoFactory = new DaoFactory();
                UtilisateurInterface utilisateurInterface = daoFactory.getUtilisateurInterface();

                user = utilisateurInterface.createUtilisateur( user );

                Response response = Response
                                .status(Response.Status.CREATED)
                                .entity("L'utilisateur à bien été créer." + user)
                                .build();
                return response;
        }

        /**
         * Mettre à jour un utilisateur quand on n'est un administrateur.
         * @param user
         * @return 
         */
        @Path("/admin/update/{email}")
        @PUT
        @Produces( MediaType.APPLICATION_JSON )
        @Consumes( MediaType.APPLICATION_JSON )
        public Response updateUserBd( Utilisateur user, @PathParam( value = "email" ) String email ) {
                DaoFactory daoFactory = new DaoFactory();
                EntityManager entityManager = daoFactory.getEntityManager();
                UtilisateurInterface utilisateurInterface = daoFactory.getUtilisateurInterface();

                //$ Je cherche l'utilisateur ayant l'id qui m'a été fournie dans la base de donnée.
                Query queryEmail = entityManager.createQuery( "SELECT user FROM Utilisateur user WHERE email=:email" );
                queryEmail.setParameter( "email", email );

                //: Si la liste me revient vide.
                if( queryEmail.getResultList().isEmpty() ) {
                        System.out.println( "<-----Cette adresse e-mail n'existe pas dans la base de donnée.----->" );
                        Response response = Response
                                .status( Status.FORBIDDEN )
                                .entity( "Cette adresse e-mail n'existe pas dans la base de donnée." )
                                .build();
                        return response;
                }

                Utilisateur userMaj = (Utilisateur) queryEmail.getResultList().get( 0 );
                System.out.println( "<-----Voici l'utilisateur qui est associé à cette adresse e-mail :----->\n" + userMaj.getEmail() );

                //§ Vérification de la disponibilité de l'e-mail demandé par l'utilisateur.
                queryEmail.setParameter( "email", user.getEmail() );

                //. Si la liste est vide, alors cette e-mail n'est pas encore attribué.
                if( queryEmail.getResultList().isEmpty() ) {
                        userMaj = utilisateurInterface.updateUtilisateur( user, email );
                }

                //. Si je trouve une trace de l'adresse e-mail dans la base de donnée : je vérifie si c'est celle de l'utilisateur, et si il souhaite la conserver.
                else {
                        Utilisateur userBDD = (Utilisateur) queryEmail.getResultList().get(0);
                        String emailUserBDD = userBDD.getEmail();

                        //. Si oui alors je le laisse faire.
                        if ( emailUserBDD.equals(user.getEmail()) ) {
                                userMaj = utilisateurInterface.updateUtilisateur( user, email );
                        }

                        //. S'il veut en changer mais qu'il est déjà utilisé dans la base de donnée alors je le bloque.
                        else {
                                Response responseErreur = Response
                                        .status( Status.FORBIDDEN )
                                        .entity( "L'adresse e-mail " + user.getEmail() + " est déjà utilisé par un autre utilisateur." )
                                        .build();
                                return responseErreur;
                        }
                }

                Response response = Response
                        .ok( "L'utilisateur " + userMaj.getEmail()  + " à été mis à jour avec succès." )
                        .build();
                return response;
        }

        /**
         * Permet de désactiver un utilisateur.
         * @param email
         * @return
         */
        @Path("/admin/deactivate/{email}")
        @PUT
        @Produces( MediaType.APPLICATION_JSON )
        @Consumes( MediaType.APPLICATION_JSON )
        public Response deactivateUserBd( @PathParam( value = "email" ) String email ) {
                DaoFactory daoFactory = new DaoFactory();
                EntityManager entityManager = daoFactory.getEntityManager();
                UtilisateurInterface utilisateurInterface = daoFactory.getUtilisateurInterface();

                //§ Je cherche l'utilisateur ayant l'email fournit dans la base de donnée.
                Query query = entityManager.createQuery( "SELECT user FROM Utilisateur user WHERE email=:email" );
                query.setParameter( "email", email );

                //: Si la liste me revient vide.
                if( query.getResultList().isEmpty() ) {
                        System.out.println( "<-----Cette e-mail n'existe pas dans la base de donnée.----->" );
                        Response response = Response
                                .status( Status.FORBIDDEN )
                                .entity( "Cette adresse e-mail n'existe pas dans la base de donnée." )
                                .build();
                        return response;
                }

                Utilisateur userADesactiver = ( Utilisateur ) query.getResultList().get( 0 );
                System.out.println( "<-----Voici l'utilisateur qui est associé à cette adresse e-mail :----->\n" + userADesactiver.getEmail() );

                userADesactiver = utilisateurInterface.deactivateUtilisateur(email);

                Response response = Response
                        .ok( "L'utilisateur " + userADesactiver.getEmail() + " à été désactivé avec succès." )
                        .build();
                return response;
        }

        /**
         * Permet d'activer un utilisateur.
         * @param user
         * @param email
         * @return
         */
        @Path("/admin/activate/{email}")
        @PUT
        @Produces( MediaType.APPLICATION_JSON )
        @Consumes( MediaType.APPLICATION_JSON )
        public Response activateUserBd( Utilisateur user, @PathParam( value = "email" ) String email ) {
                DaoFactory daoFactory = new DaoFactory();
                EntityManager entityManager = daoFactory.getEntityManager();
                UtilisateurInterface utilisateurInterface = daoFactory.getUtilisateurInterface();

                //§ Je cherche l'utilisateur ayant l'email fournit dans la base de donnée.
                Query query = entityManager.createQuery( "SELECT user FROM Utilisateur user WHERE email=:email" );
                query.setParameter( "email", email );

                //: Si la liste me revient vide.
                if( query.getResultList().isEmpty() ) {
                        System.out.println( "<-----Cette e-mail n'existe pas dans la base de donnée.----->" );
                        Response response = Response
                                .status( Status.FORBIDDEN )
                                .entity( "Cette adresse e-mail n'existe pas dans la base de donnée." )
                                .build();
                        return response;
                }

                Utilisateur userAActiver = ( Utilisateur ) query.getResultList().get( 0 );
                System.out.println( "<-----Voici l'utilisateur qui est associé à cette id :----->\n" + userAActiver.getEmail());

                userAActiver = utilisateurInterface.reactivateUtilisateur(email);

                Response response = Response
                        .ok( "L'utilisateur " + userAActiver.getEmail() + " à été activé avec succès." )
                        .build();
                return response;
        }

        /**
         * Supprimer un utilisateur.
         * @param id
         * @return 
         */
        @Path("/admin/delete/{id}")
        @DELETE
        @Produces( MediaType.APPLICATION_JSON )
        public Response deleteUserBd( @PathParam( value = "id" ) int id ) {
                DaoFactory daoFactory = new DaoFactory();
                EntityManager entityManager = daoFactory.getEntityManager();
                UtilisateurInterface utilisateurInterface = daoFactory.getUtilisateurInterface();
                //$ Je cherche l'utilisateur ayant l'id qui m'a été fournie dans la base de donnée.
                Query query = entityManager.createQuery( "SELECT user FROM Utilisateur user WHERE id=:id" );
                query.setParameter( "id", id );

                //: Si ma liste me revient vide.
                if( query.getResultList().isEmpty() ) {
                        System.out.println( "<-----Cette id n'existe pas dans la base de donnée.----->" );
                        Response response = Response
                                .status( Status.FORBIDDEN )
                                .entity( "Cette id n'existe pas dans la base de donnée." )
                                .build();
                        return response;
                }

                Utilisateur userDelete = (Utilisateur) query.getResultList().get( 0 );
                System.out.println( "<-----Voici l'utilisateur qui est associé à cette id :----->\n" + userDelete.getEmail() );

                userDelete = utilisateurInterface.deleteUtilisateur(id);

                Response response = Response
                        .ok( "L'utilisateur a bien été supprimé de la base de donnée." )
                        .build();
                return response;
        }
}